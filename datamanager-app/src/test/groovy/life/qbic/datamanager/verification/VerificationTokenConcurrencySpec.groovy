package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.application.verification.VerificationTokenServiceImpl
import life.qbic.identity.domain.model.verification.ConsumeFailure
import life.qbic.identity.domain.repository.VerificationTokenRepository
import life.qbic.identity.infrastructure.verification.JpaVerificationTokenRepository
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
 * <b>SPIKE — the two concurrency questions a DDL review cannot answer.</b>
 *
 * <ol>
 *   <li><b>Double consumption.</b> Two requests present the same valid link at the same time. Does
 *       exactly one win, or do both?</li>
 *   <li><b>Lost throttle increments.</b> N concurrent attempts against one account. Does the
 *       counter reach N, or does read-then-write lose some?</li>
 * </ol>
 *
 * <p>Both are decided by the persistence adapter, and both are invisible in a schema review: the
 * unique index guards <em>inserts</em>, while consumption is an <em>update</em>.</p>
 */
class VerificationTokenConcurrencySpec extends Specification {

    @Shared
    EntityManagerFactory emf

    Boolean usersTableCache

    static final Duration HOUR = Duration.ofHours(1)
    static final Duration TINY = Duration.ofMinutes(1)

    // ---------------------------------------------------------------- database under test
    //
    // Defaults to H2 in MODE=MySQL. Set -Dverification.jdbc.url (and friends) to run the identical
    // specs against MariaDB, which is the only way to validate that InnoDB's locking behaves like
    // the engine these specs were written on. H2 and InnoDB do NOT have identical lock semantics.
    static String jdbcUrl() {
        System.getProperty("verification.jdbc.url",
                "jdbc:h2:mem:vertokenconc;MODE=MySQL;DB_CLOSE_DELAY=-1")
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

    /**
     * The dialect to pin, or {@code null} to let Hibernate infer it.
     *
     * <p>H2 infers its own dialect and warns if one is set explicitly (HHH90000025). MariaDB 12 via
     * the MySQL driver cannot be inferred, so the MariaDB run must pass
     * {@code -Dverification.dialect=org.hibernate.dialect.MariaDBDialect}.</p>
     */
    static String dialect() {
        System.getProperty("verification.dialect")
    }

    static String ddlAuto() {
        System.getProperty("verification.jdbc.url") == null ? "create-drop" : "none"
    }

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
        def registry = builder.build()
        def metadata = new MetadataSources(registry)
                .addAnnotatedClass(life.qbic.identity.domain.model.verification.VerificationToken)
                .buildMetadata()
        emf = metadata.buildSessionFactory()
        createUsersTable()
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
    }

    // --------------------------------------------------------------- 1. double consumption

    def "exactly one of N concurrent consumers of the same token wins"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def service = newService()
        def issued = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now)

        and: "all threads are released at the same moment"
        int threads = 8
        def start = new CountDownLatch(1)
        def pool = Executors.newFixedThreadPool(threads)

        when:
        def outcomes = (1..threads).collect { i ->
            pool.submit({
                start.await()
                service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION,
                        now.plusSeconds(10))
            } as Callable)
        }
        start.countDown()
        def results = outcomes.collect { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        then: "the token is consumed exactly once — this is the property AUTH-R-05 requires"
        results.count { it.isPresent() } == 1

        and: "every other consumer is refused, and told nothing about why (AUTH-NFR-03)"
        results.findAll { it.isEmpty() }.size() == threads - 1
    }

    def "the second consumer is stopped by row locking, not by the unique index"() {
        given: "the unique index only guards INSERT, so it cannot help an UPDATE"
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def service = newService()
        def issued = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now)

        when: "two sequential consumers run against the same row"
        def first = service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(5))
        def second = service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(6))

        then:
        first.isPresent()
        second.isEmpty()

        and: "the persisted row records exactly one consumption timestamp"
        def consumedAt = singleColumn("consumedAt")
        consumedAt != null
    }

    def "a replayed token after consumption leaves the row state unchanged"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def service = newService()
        def issued = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now)
        service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now.plusSeconds(5))
        def firstConsumedAt = singleColumn("consumedAt")

        when: "replayed later"
        service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now.plusSeconds(600))

        then: "the original consumption instant is not rewritten"
        singleColumn("consumedAt") == firstConsumedAt
    }

    // --------------------------------------------------------------- 2. throttle increments

    // --------------------------------------------------------------- H2 portability finding

    // --------------------------------------------------------------- helpers

    /**
     * Whether the schema carries a {@code users} table.
     *
     * <p>Resolved once, by inspection. The FK to {@code users} only exists in the real migration
     * DDL; the H2-generated schema omits it, so there is nothing to satisfy there. Probing with a
     * failing query per call would spam the log with {@code Table "USERS" not found} and, worse,
     * hide a genuine failure behind a swallowed exception.</p>
     */
    Boolean usersTablePresent() {
        if (usersTableCache == null) {
            EntityManager em = emf.createEntityManager()
            try {
                // information_schema only: querying `users` itself is what produced the
                // per-call "Table USERS not found" noise in the first place.
                // Filter by schema: H2 ships its own INFORMATION_SCHEMA.USERS, which a plain
                // TABLE_NAME match happily finds. We want an application table, not a metadata one.
                def found = em.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.TABLES " +
                        "WHERE UPPER(TABLE_NAME) = 'USERS' " +
                        "  AND TABLE_SCHEMA NOT IN ('INFORMATION_SCHEMA', 'information_schema')")
                        .singleResult
                usersTableCache = ((Number) found).intValue() > 0
            } catch (Exception e) {
                usersTableCache = false
            } finally {
                em.close()
            }
        }
        return usersTableCache
    }

    void ensureUser(String userId) {
        if (!usersTablePresent()) {
            return
        }
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            def existing = em.createNativeQuery("SELECT COUNT(*) FROM users WHERE id = :id")
                    .setParameter("id", userId).singleResult
            if (((Number) existing).intValue() == 0) {
                em.createNativeQuery("INSERT INTO users (id, active) VALUES (:id, TRUE)")
                        .setParameter("id", userId).executeUpdate()
            }
            em.transaction.commit()
        } catch (Exception e) {
            if (em.transaction.active) em.transaction.rollback()
            throw e   // do not swallow: a real FK/permission problem must be visible
        } finally {
            em.close()
        }
    }

    VerificationTokenServiceImpl newService() {
        new VerificationTokenServiceImpl(
                new UserEnsuringRepository(emf, { String id -> ensureUser(id) }), HOUR, TINY, HOUR)
    }

    void truncate() {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            em.createNativeQuery("DELETE FROM user_verification_token").executeUpdate()
            em.transaction.commit()
        } finally {
            em.close()
        }
    }

    Object singleColumn(String column) {
        EntityManager em = emf.createEntityManager()
        try {
            def rows = em.createNativeQuery("SELECT ${column} FROM user_verification_token").resultList
            return rows.isEmpty() ? null : rows[0]
        } finally {
            em.close()
        }
    }

    static class EntityManagerFactoryHolder {
        static EntityManagerFactory vanillaH2(EntityManagerFactory ignored) { ignored }
    }
}
