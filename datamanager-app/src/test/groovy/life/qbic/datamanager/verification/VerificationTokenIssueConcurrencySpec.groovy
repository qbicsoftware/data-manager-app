package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.api.VerificationLinkService.Purpose
import life.qbic.identity.application.verification.VerificationTokenServiceImpl
import life.qbic.identity.domain.model.verification.VerificationToken
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * <b>Concurrent issue must not deadlock (AUTH-R-05 reissue path).</b>
 *
 * <p>Reproduces the production failure: two "request a new link" jobs for the same account ran at
 * the same time and InnoDB raised <em>"Deadlock found when trying to get lock"</em> on the token
 * INSERT. The cause is the supersede-then-insert sequence: {@code UPDATE ... WHERE userId AND
 * purpose} takes index/gap locks on the very ranges the following INSERT has to write into, and two
 * transactions doing that in opposite interleavings deadlock.</p>
 *
 * <p>Run against MariaDB for a meaningful result — H2 does not implement InnoDB's gap locking, so a
 * green H2 run proves nothing here. See {@code scripts/run-verification-spike.sh mariadb}.</p>
 */
class VerificationTokenIssueConcurrencySpec extends Specification {

    @Shared
    EntityManagerFactory emf

    static final Duration HOUR = Duration.ofHours(1)

    def setupSpec() {
        def builder = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl())
                .applySetting(AvailableSettings.JAKARTA_JDBC_USER, jdbcUser())
                .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, jdbcPassword())
                .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, jdbcDriver())
                .applySetting(AvailableSettings.HBM2DDL_AUTO, ddlAuto())
        if (dialect() != null) {
            builder.applySetting(AvailableSettings.DIALECT, dialect())
        }
        emf = new MetadataSources(builder.build())
                .addAnnotatedClass(VerificationToken)
                .buildMetadata()
                .buildSessionFactory()
        createUsersTable()
        if (ddlAuto() == "none") {
            createRealSchema()
        }
    }

    /**
     * {@code users} is needed even on H2: {@code lockAccount} issues
     * {@code SELECT id FROM users WHERE id = ? FOR UPDATE} as the per-account mutex, and the
     * generated schema has no such table because {@code User} belongs to a different persistence
     * unit. Production always has it; this is test scaffolding only.
     */
    void createUsersTable() {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            em.createNativeQuery("""
                CREATE TABLE IF NOT EXISTS users (
                    id varchar(255) NOT NULL PRIMARY KEY,
                    active boolean NOT NULL
                )
                """).executeUpdate()
            em.transaction.commit()
        } catch (Exception ignored) {
            if (em.transaction.active) em.transaction.rollback()
        } finally {
            em.close()
        }
    }

    def cleanupSpec() {
        emf?.close()
    }

    def setup() {
        truncate()
        ensureUser("u-race")
    }

    /**
     * The real DDL has FK_user_verification_token_user, so the owning user must exist. Invisible on
     * H2, whose generated schema omits the FK. Production is unaffected: a token is only issued for
     * a registered account.
     */
    void ensureUser(String userId) {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            try {
                def found = em.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.TABLES " +
                        "WHERE UPPER(TABLE_NAME) = 'USERS' " +
                        "  AND TABLE_SCHEMA NOT IN ('INFORMATION_SCHEMA', 'information_schema')")
                        .singleResult
                if (((Number) found).intValue() > 0) {
                    def existing = em.createNativeQuery("SELECT COUNT(*) FROM users WHERE id = :id")
                            .setParameter("id", userId).singleResult
                    if (((Number) existing).intValue() == 0) {
                        em.createNativeQuery("INSERT INTO users (id, active) VALUES (:id, TRUE)")
                                .setParameter("id", userId).executeUpdate()
                    }
                }
            } catch (Exception e) {
                throw e
            }
            em.transaction.commit()
        } finally {
            em.close()
        }
    }

    def "many concurrent reissues for one account all succeed"() {
        given: "one account requesting a new link repeatedly, concurrently"
        int threads = 6
        def start = new CountDownLatch(1)
        def pool = Executors.newFixedThreadPool(threads)
        def now = Instant.parse("2026-11-10T10:00:00Z")

        when:
        def tasks = (1..threads).collect {
            pool.submit({
                start.await()
                // One transaction per issue call, as @Transactional gives in production.
                try {
                    new ServiceFactory(emf).inTransaction({ service ->
                        service.issue("u-race", Purpose.EMAIL_CONFIRMATION, now)
                    })
                } catch (Throwable t) {
                    return t
                }
                return null
            } as Callable)
        }
        start.countDown()
        def failures = tasks.collect { it.get(60, TimeUnit.SECONDS) }.findAll { it != null }
        pool.shutdown()

        then: "no deadlock and no other failure — a reissue must be safe to retry concurrently"
        failures.isEmpty()

        and: "exactly one token for the account is left usable"
        usableTokenCount() == 1

        and: "every issued token is still recorded, superseded or not"
        totalTokenCount() == threads
    }

    // ------------------------------------------------------------------ helpers

    static String jdbcUrl() {
        System.getProperty("verification.jdbc.url", "jdbc:h2:mem:vertokenissue;MODE=MySQL;DB_CLOSE_DELAY=-1")
    }

    static String jdbcUser() {
        System.getProperty("verification.jdbc.user", "sa")
    }

    static String jdbcPassword() {
        System.getProperty("verification.jdbc.password", "")
    }

    static String jdbcDriver() {
        System.getProperty("verification.jdbc.driver", "org.h2.Driver")
    }

    static String dialect() {
        System.getProperty("verification.dialect")
    }

    static String ddlAuto() {
        System.getProperty("verification.jdbc.url") == null ? "create-drop" : "none"
    }

    void createRealSchema() {
        def f = new File("sql/migrations/auth-flow-hardening.sql")
        if (!f.exists()) {
            f = new File("../sql/migrations/auth-flow-hardening.sql")
        }
        def em = emf.createEntityManager()
        try {
            em.transaction.begin()
            f.text.readLines().findAll { !it.trim().startsWith("--") && !it.trim().isEmpty() }
                    .join("\n").split(";").each { stmt ->
                        if (!stmt.trim().isEmpty()) {
                            em.createNativeQuery(stmt).executeUpdate()
                        }
                    }
            em.transaction.commit()
        } finally {
            em.close()
        }
    }

    int usableTokenCount() {
        query("SELECT COUNT(*) FROM user_verification_token " +
                "WHERE consumedAt IS NULL AND supersededAt IS NULL")
    }

    int totalTokenCount() {
        query("SELECT COUNT(*) FROM user_verification_token")
    }

    int query(String sql) {
        EntityManager em = emf.createEntityManager()
        try {
            return ((Number) em.createNativeQuery(sql).singleResult).intValue()
        } finally {
            em.close()
        }
    }

    void truncate() {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            em.createNativeQuery("DELETE FROM user_verification_token").executeUpdate()
            em.transaction.commit()
        } catch (Exception ignored) {
            if (em.transaction.active) em.transaction.rollback()
        } finally {
            em.close()
        }
    }
}
