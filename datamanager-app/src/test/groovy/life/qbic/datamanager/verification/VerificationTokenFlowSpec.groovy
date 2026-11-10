package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.application.verification.VerificationTokenServiceImpl
import life.qbic.identity.domain.model.verification.ConsumeFailure
import life.qbic.identity.infrastructure.verification.JpaVerificationTokenRepository
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * <b>SPIKE — verification token flow against real persistence.</b>
 *
 * <p>Answers the questions a DDL review cannot: does the round-trip through Hibernate + a real
 * database preserve the properties the schema was chosen for?</p>
 *
 * <p>Runs on H2 in {@code MODE=MySQL} because {@link life.qbic.identity.infrastructure.verification.JpaAttemptCounter}
 * relies on MySQL/MariaDB {@code ON DUPLICATE KEY UPDATE}; plain H2 rejects that syntax (verified on
 * H2 2.3.232). Without the mode flag these tests would exercise a statement production never runs.</p>
 *
 * <p>In production these specs belong in {@code identity-infrastructure}; they live here in the spike
 * because that module has no test dependencies configured yet.</p>
 */
class VerificationTokenFlowSpec extends Specification {

    @Shared
    EntityManagerFactory emf

    Boolean usersTableCache

    VerificationTokenServiceImpl service

    static final Duration CONFIRMATION = Duration.ofHours(1)
    static final Duration RESET = Duration.ofMinutes(5)
    static final Duration SET_PASSWORD = Duration.ofHours(1)

    // ---------------------------------------------------------------- database under test
    //
    // Defaults to H2 in MODE=MySQL. Set -Dverification.jdbc.url (and friends) to run the identical
    // specs against MariaDB, which is the only way to validate that InnoDB's locking behaves like
    // the engine these specs were written on. H2 and InnoDB do NOT have identical lock semantics.
    static String jdbcUrl() {
        System.getProperty("verification.jdbc.url",
                "jdbc:h2:mem:vertoken;MODE=MySQL;DB_CLOSE_DELAY=-1")
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
     * On H2 the schema is generated from the entity. On MariaDB it must come from the real migration
     * DDL, because that is what production runs — generating it would test Hibernate's idea of the
     * schema rather than the signed-off one.
     */
    /**
     * Hibernate cannot infer a dialect from a MariaDB 12 connection through the MySQL driver, so a
     * MariaDB run must state it. The application pins {@code MariaDBDialect} in
     * {@code application.properties}; these specs mirror that.
     */
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
        if (ddlAuto() == "none") {
            createRealSchema()
        }
    }

    /**
     * Applies the migration draft's DDL verbatim, so a MariaDB run exercises the signed-off schema
     * including {@code char(64)} and the unique keys — not a Hibernate approximation.
     */
    void createRealSchema() {
        // surefire runs with the module as working directory, so walk up to the repo root.
        def f = new File("sql/migrations/drafts/auth-flow-hardening.sql")
        if (!f.exists()) {
            f = new File("../sql/migrations/drafts/auth-flow-hardening.sql")
        }
        def ddl = f.text
        // The script is idempotent and only needs the two CREATE TABLE blocks; run it whole.
        def em = emf.createEntityManager()
        try {
            em.transaction.begin()
            ddl.readLines().findAll { !it.trim().startsWith("--") && !it.trim().isEmpty() }
                    .join("\n")
                    .split(";")
                    .each { stmt ->
                        if (!stmt.trim().isEmpty()) {
                            em.createNativeQuery(stmt).executeUpdate()
                        }
                    }
            em.transaction.commit()
        } finally {
            em.close()
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
        service = newService(CONFIRMATION, RESET, SET_PASSWORD)
    }

    /**
     * Builds the service with one EntityManager per repository call.
     *
     * <p>This mirrors a request-scoped EntityManager: each call is its own unit of work. Real
     * production wiring would need consumption and the state change it authorises (confirming an
     * address, setting a password) in ONE transaction — see the spike notes.</p>
     */
    VerificationTokenServiceImpl newService(Duration confirmation, Duration reset, Duration setPw) {
        new VerificationTokenServiceImpl(new UserEnsuringRepository(emf, { String id -> ensureUser(id) }),
                confirmation, reset, setPw)
    }

    /**
     * Ensures the owning user row exists before a token is persisted.
     *
     * <p>Invisible on H2 (generated schema has no FK), required on MariaDB, where the real DDL's
     * {@code FK_user_verification_token_user REFERENCES users(id)} rejects the insert otherwise.
     * In production the user always exists: a token is only issued for a registered account.</p>
     */
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

    def truncate() {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            em.createNativeQuery("DELETE FROM user_verification_token").executeUpdate()
            em.transaction.commit()
        } finally {
            em.close()
        }
    }

    // ---------------------------------------------------------------- happy path

    def "a confirmation token is usable exactly once"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now)

        when: "first use"
        def first = service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(60))

        then:
        first.isPresent()
        first.get() == "u-1"

        when: "second use of the same link"
        def second = service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(61))

        then: "the link is dead — this is AUTH-R-05's single-use clause"
        second.isEmpty()
    }

    def "the raw token is never stored, only its SHA-256 hash"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")

        when:
        def issued = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now)

        then: "the raw token does not appear anywhere in the table"
        !persistedColumnValues().contains(issued.rawToken())

        and: "the stored hash is the SHA-256 of the raw token"
        persistedTokenHashes() == [life.qbic.identity.domain.model.verification.VerificationTokens.hash(issued.rawToken())]
    }

    def "a token is rejected after its configured lifetime"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now)

        expect: "the reset lifetime is 5 minutes here"
        issued.expiresAt() == now.plus(RESET)

        when: "consumed one second past expiry"
        def outcome = service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now.plus(RESET).plusSeconds(1))

        then:
        outcome.isEmpty()
    }

    def "a token is still usable one second before expiry"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now)

        when:
        def outcome = service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET,
                now.plus(RESET).minusSeconds(1))

        then:
        outcome.isPresent()
    }

    // ---------------------------------------------------------------- supersede

    def "issuing a new token for the same purpose invalidates the previous one"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def first = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now)
        def second = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(30))

        when: "the older link is followed"
        def oldOutcome = service.consume(first.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(60))

        then:
        oldOutcome.isEmpty()

        when: "the newer link is followed"
        def newOutcome = service.consume(second.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(61))

        then:
        newOutcome.isPresent()
    }

    def "superseding does not touch a different purpose for the same user"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def confirmation = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now)
        def reset = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now)

        when: "a second reset is issued, superseding the first reset"
        service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now.plusSeconds(10))

        then: "the confirmation token is untouched"
        def outcome = service.consume(confirmation.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(60))
        outcome.isPresent()

        and: "the first reset token was superseded"
        def resetOutcome = service.consume(reset.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now.plusSeconds(60))
        resetOutcome.isEmpty()
    }

    // ---------------------------------------------------------------- cross-purpose

    def "a confirmation token cannot be used to reset a password"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def confirmation = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now)

        when: "presented at the reset endpoint"
        def outcome = service.consume(confirmation.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now.plusSeconds(10))

        then:
        outcome.isEmpty()

        and: "and it is still usable for its own purpose — a wrong attempt must not burn it"
        service.consume(confirmation.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now.plusSeconds(11)).isPresent()
    }

    def "an unknown token is reported without touching anything"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")

        when:
        def outcome = service.consume("not-a-real-token", life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, now)

        then:
        outcome.isEmpty()
    }

    def "blank and null tokens are rejected"() {
        expect:
        service.consume(raw, life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION, Instant.now()).isEmpty()

        where:
        raw << [null, "", "   "]
    }

    // ---------------------------------------------------------------- time handling

    def "expiry survives a round-trip through the database regardless of JVM default timezone"() {
        given: "the same absolute instant expressed via a non-UTC default zone"
        def originalZone = TimeZone.getDefault()
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = service.issue("u-1", life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now)

        when:
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Pacific/Kiritimati"))) // UTC+14
        def outcome = service.consume(issued.rawToken(), life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET, now.plusSeconds(60))

        then: "expiry is evaluated in one clock, so the offset cannot make a valid token look expired"
        outcome.isPresent()

        cleanup:
        TimeZone.setDefault(originalZone)
    }

    // ---------------------------------------------------------------- bounds

    def "a configured lifetime beyond the AUTH-R-05 bound fails at construction"() {
        when:
        newService(confirmation, reset, setPassword)

        then:
        thrown(IllegalArgumentException)

        where:
        confirmation         | reset                       | setPassword
        Duration.ofHours(2)  | Duration.ofMinutes(5)       | Duration.ofHours(1)   // confirmation > 1h
        Duration.ofHours(1)  | Duration.ofMinutes(16)      | Duration.ofHours(1)   // reset > 15min
        Duration.ofHours(1)  | Duration.ofMinutes(5)       | Duration.ofHours(2)   // set-password > 1h
    }

    def "a token hash that is not a 64-char lower-case hex digest is rejected"() {
        when:
        life.qbic.identity.domain.model.verification.VerificationToken.issue(
                "u-1", life.qbic.identity.domain.model.verification.TokenPurpose.EMAIL_CONFIRMATION,
                hash, Instant.now(), CONFIRMATION)

        then:
        thrown(IllegalArgumentException)

        where:
        hash << [
                "too-short",
                "A" * 64,                       // upper-case is not accepted
                life.qbic.identity.domain.model.verification.VerificationTokens.newRawToken()
        ]
    }

    // ---------------------------------------------------------------- helpers

    List<String> persistedTokenHashes() {
        EntityManager em = emf.createEntityManager()
        try {
            em.createNativeQuery("SELECT tokenHash FROM user_verification_token")
                    .resultList.collect { ((String) it).trim() }
        } finally {
            em.close()
        }
    }

    List<String> persistedColumnValues() {
        EntityManager em = emf.createEntityManager()
        try {
            em.createNativeQuery("SELECT * FROM user_verification_token").resultList
                    .collect { row -> ((Object[]) row).collect { String.valueOf(it) } }
                    .flatten()
                    .collect { it.trim() }
        } finally {
            em.close()
        }
    }

}
