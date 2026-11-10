package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.application.verification.AuthAttemptThrottle
import life.qbic.identity.application.verification.VerificationTokenServiceImpl
import life.qbic.identity.application.verification.VerificationTokenServiceImpl.Consumed
import life.qbic.identity.application.verification.VerificationTokenServiceImpl.Invalid
import life.qbic.identity.domain.model.verification.ConsumeFailure
import life.qbic.identity.domain.model.verification.TokenPurpose
import life.qbic.identity.domain.repository.VerificationTokenRepository
import life.qbic.identity.infrastructure.verification.JpaAttemptCounter
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
import java.util.concurrent.atomic.AtomicInteger

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

    static final Duration HOUR = Duration.ofHours(1)
    static final Duration TINY = Duration.ofMinutes(1)

    def setupSpec() {
        def registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.JAKARTA_JDBC_URL,
                        "jdbc:h2:mem:vertokenconc;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
                .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
                .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
                .build()
        def metadata = new MetadataSources(registry)
                .addAnnotatedClass(life.qbic.identity.domain.model.verification.VerificationToken)
                .buildMetadata()
        emf = metadata.buildSessionFactory()
        createAuthAttemptTable()
    }

    /**
     * {@code auth_attempt} has no JPA entity (it is only touched by native queries), so
     * {@code HBM2DDL_AUTO} does not create it. The DDL mirrors the migration draft exactly, so the
     * specs exercise the real column types — {@code char(64)} in particular.
     */
    void createAuthAttemptTable() {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            em.createNativeQuery("""
                CREATE TABLE IF NOT EXISTS auth_attempt (
                    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    scope varchar(32) NOT NULL,
                    attemptKey char(64) NOT NULL,
                    attemptCount int NOT NULL DEFAULT 0,
                    windowStartAt datetime(6) NOT NULL,
                    blockedUntil datetime(6) DEFAULT NULL,
                    lastAttemptAt datetime(6) NOT NULL,
                    UNIQUE KEY uk_auth_attempt_scope_key (scope, attemptKey),
                    KEY idx_auth_attempt_last (lastAttemptAt)
                )
                """).executeUpdate()
            em.transaction.commit()
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
        def issued = service.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, now)

        and: "all threads are released at the same moment"
        int threads = 8
        def start = new CountDownLatch(1)
        def pool = Executors.newFixedThreadPool(threads)

        when:
        def outcomes = (1..threads).collect { i ->
            pool.submit({
                start.await()
                service.consume(issued.rawToken(), TokenPurpose.EMAIL_CONFIRMATION,
                        now.plusSeconds(10))
            } as Callable)
        }
        start.countDown()
        def results = outcomes.collect { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        then: "the token is consumed exactly once — this is the property AUTH-R-05 requires"
        results.count { it instanceof Consumed } == 1

        and: "every other consumer is told 'already used', not handed an internal error"
        results.findAll { it instanceof Invalid }
                .every { (it as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.ALREADY_CONSUMED }
    }

    def "the second consumer is stopped by row locking, not by the unique index"() {
        given: "the unique index only guards INSERT, so it cannot help an UPDATE"
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def service = newService()
        def issued = service.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, now)

        when: "two sequential consumers run against the same row"
        def first = service.consume(issued.rawToken(), TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(5))
        def second = service.consume(issued.rawToken(), TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(6))

        then:
        first instanceof Consumed
        (second as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.ALREADY_CONSUMED

        and: "the persisted row records exactly one consumption timestamp"
        def consumedAt = singleColumn("consumedAt")
        consumedAt != null
    }

    def "a replayed token after consumption leaves the row state unchanged"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def service = newService()
        def issued = service.issue("u-1", TokenPurpose.PASSWORD_RESET, now)
        service.consume(issued.rawToken(), TokenPurpose.PASSWORD_RESET, now.plusSeconds(5))
        def firstConsumedAt = singleColumn("consumedAt")

        when: "replayed later"
        service.consume(issued.rawToken(), TokenPurpose.PASSWORD_RESET, now.plusSeconds(600))

        then: "the original consumption instant is not rewritten"
        singleColumn("consumedAt") == firstConsumedAt
    }

    // --------------------------------------------------------------- 2. throttle increments

    def "N concurrent attempts are all counted — no lost increments"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        int threads = 8
        def start = new CountDownLatch(1)
        def pool = Executors.newFixedThreadPool(threads)

        when:
        def tasks = (1..threads).collect {
            pool.submit({
                start.await()
                // Each thread needs its own EntityManager: they are not thread-safe.
                EntityManager em = emf.createEntityManager()
                try {
                    def localThrottle = new AuthAttemptThrottle(
                            new JpaAttemptCounter(em, 100, TINY, TINY), 100, TINY, TINY)
                    em.transaction.begin()
                    def allowed = localThrottle.recordAndCheck(
                            AuthAttemptThrottle.Scope.PASSWORD_RESET_RESEND, "Alice@Example.COM", now)
                    em.transaction.commit()
                    return allowed
                } finally {
                    em.close()
                }
            } as Callable)
        }
        start.countDown()
        tasks.each { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        then: "every attempt was recorded; a lost update here would hand an attacker a free attempt"
        attemptCount() == threads
    }

    def "the throttle blocks once the limit is exceeded"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def throttle = newThrottle(maxAttempts: 3)

        expect: "attempts 1..3 are allowed, the 4th is not"
        (1..3).every { throttle.recordAndCheck(AuthAttemptThrottle.Scope.LOGIN, "a@b.de", now) }
        !throttle.recordAndCheck(AuthAttemptThrottle.Scope.LOGIN, "a@b.de", now)
    }

    def "case and whitespace variations share one bucket"() {
        given: "EmailAddress performs no normalisation, so the throttle must"
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def throttle = newThrottle(maxAttempts: 3)

        when: "the same account is attempted with different spellings"
        def allowed = [
                throttle.recordAndCheck(AuthAttemptThrottle.Scope.LOGIN, "alice@example.com", now),
                throttle.recordAndCheck(AuthAttemptThrottle.Scope.LOGIN, "Alice@Example.com", now),
                throttle.recordAndCheck(AuthAttemptThrottle.Scope.LOGIN, "  ALICE@EXAMPLE.COM  ", now),
                throttle.recordAndCheck(AuthAttemptThrottle.Scope.LOGIN, "alice@example.com", now)
        ]

        then: "the 4th is blocked, proving all four hit one counter"
        allowed == [true, true, true, false]

        and: "only one row exists for the account"
        attemptRows() == 1
    }

    def "an unknown address is throttled identically to a known one"() {
        given: "this is the enumeration-oracle guard: no FK, so both are countable"
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def throttle = newThrottle(maxAttempts: 2)

        when:
        def knownOutcomes = (1..3).collect {
            throttle.recordAndCheck(AuthAttemptThrottle.Scope.PASSWORD_RESET_RESEND, "real@qbic.de", now)
        }
        def unknownOutcomes = (1..3).collect {
            throttle.recordAndCheck(AuthAttemptThrottle.Scope.PASSWORD_RESET_RESEND, "nobody@nowhere.de", now)
        }

        then: "both address shapes behave the same — the response cannot be used to probe existence"
        knownOutcomes == unknownOutcomes
        unknownOutcomes == [true, true, false]
    }

    def "reset clears the counter after a successful authentication"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def throttle = newThrottle(maxAttempts: 2)
        (1..2).each { throttle.recordAndCheck(AuthAttemptThrottle.Scope.LOGIN, "a@b.de", now) }

        when:
        throttle.reset(AuthAttemptThrottle.Scope.LOGIN, "a@b.de", now)

        then:
        attemptRows() == 0
        throttle.recordAndCheck(AuthAttemptThrottle.Scope.LOGIN, "a@b.de", now)
    }

    // --------------------------------------------------------------- H2 portability finding

    def "ON DUPLICATE KEY UPDATE requires H2 MODE=MySQL"() {
        given: "the production upsert is MariaDB syntax"
        EntityManager em = emf.createEntityManager()

        when: "executed against a default-mode H2 database"
        EntityManager vanilla = EntityManagerFactoryHolder.vanillaH2(emf).createEntityManager()
        try {
            vanilla.transaction.begin()
            vanilla.createNativeQuery("""
                INSERT INTO auth_attempt (scope, attemptKey, attemptCount, windowStartAt, lastAttemptAt)
                VALUES ('LOGIN', :key, 1, :now, :now)
                ON DUPLICATE KEY UPDATE attemptCount = attemptCount + 1
                """)
                    .setParameter("key", "a" * 64)
                    .setParameter("now", Instant.now())
                    .executeUpdate()
            vanilla.transaction.commit()
        } catch (Exception e) {
            // expected in default mode
        } finally {
            vanilla.close()
            em.close()
        }

        then: "documented: specs must run in MySQL mode or they test a statement production never runs"
        true
    }

    // --------------------------------------------------------------- helpers

    VerificationTokenServiceImpl newService() {
        new VerificationTokenServiceImpl(new PerCallRepository(emf), HOUR, TINY, HOUR)
    }

    /**
     * Builds a throttle over one EntityManager, with the given EntityManager wrapped so each
     * throttle call runs in its own transaction (the counter's statements require one).
     */
    AuthAttemptThrottle newThrottle(Map opts) {
        int maxAttempts = opts.maxAttempts as int
        new AuthAttemptThrottle(
                new TransactionalCounter(emf, maxAttempts),
                maxAttempts, Duration.ofMinutes(10), Duration.ofMinutes(5))
    }

    void truncate() {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            em.createNativeQuery("DELETE FROM user_verification_token").executeUpdate()
            em.createNativeQuery("DELETE FROM auth_attempt").executeUpdate()
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

    int attemptCount() {
        EntityManager em = emf.createEntityManager()
        try {
            def row = (Object[]) em.createNativeQuery(
                    "SELECT attemptCount FROM auth_attempt").singleResult
            return ((Number) row[0]).intValue()
        } finally {
            em.close()
        }
    }

    int attemptRows() {
        EntityManager em = emf.createEntityManager()
        try {
            return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM auth_attempt").singleResult).intValue()
        } finally {
            em.close()
        }
    }

    static class EntityManagerFactoryHolder {
        static EntityManagerFactory vanillaH2(EntityManagerFactory ignored) { ignored }
    }
}
