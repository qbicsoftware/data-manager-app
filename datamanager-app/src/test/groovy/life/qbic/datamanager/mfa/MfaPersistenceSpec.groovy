package life.qbic.datamanager.mfa

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.domain.model.mfa.MfaGate
import life.qbic.identity.domain.model.mfa.MfaGateDecision
import life.qbic.identity.domain.model.mfa.MfaGrace
import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.RecoveryCode
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.repository.MfaGraceRepository
import life.qbic.identity.domain.repository.RecoveryCodeRepository
import life.qbic.identity.domain.repository.SecondFactorRepository
import life.qbic.identity.infrastructure.mfa.JpaMfaGraceRepository
import life.qbic.identity.infrastructure.mfa.JpaRecoveryCodeRepository
import life.qbic.identity.infrastructure.mfa.JpaSecondFactorRepository
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
 * <b>The storage layer the factor, the recovery path and the grace window actually run on (M2).</b>
 *
 * <p>Answers what a DDL review cannot: that a seed survives the round trip as a {@link ProtectedSecret},
 * that a <em>disabled</em> factor stops counting as enrolment, that one recovery code cannot be spent
 * twice — including under concurrency — and that the grace window is written once and then never moves
 * again.</p>
 *
 * <p>Runs on H2 in {@code MODE=MySQL} because {@code JpaMfaGraceRepository.startIfAbsent} relies on
 * MariaDB/MySQL {@code ON DUPLICATE KEY UPDATE}; plain H2 rejects that syntax (H2 2.3.232). Without the
 * mode flag this spec would exercise a statement production never runs. The FK and CHECK constraints in
 * {@code sql/migrations/drafts/mfa-second-factor.sql} are <em>not</em> covered here: H2 generates the
 * schema from the entities. They are validated when the DDL is applied to MariaDB.</p>
 *
 * <p>These specs live in {@code datamanager-app} because {@code identity-infrastructure} has no test
 * dependencies configured; they exercise that module's adapters directly.</p>
 */
class MfaPersistenceSpec extends Specification {

    @Shared
    EntityManagerFactory emf

    static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z")
    static final Duration TWO_DAYS = Duration.ofHours(48)
    static final String USER = "u-1"

    // 12-byte nonce + 16-byte tag + payload
    static final ProtectedSecret SEED =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

    // ---------------------------------------------------------------- database under test

    static String jdbcUrl() {
        System.getProperty("mfa.jdbc.url", "jdbc:h2:mem:mfa;MODE=MySQL;DB_CLOSE_DELAY=-1")
    }

    def setupSpec() {
        def builder = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl())
                .applySetting(AvailableSettings.JAKARTA_JDBC_USER,
                        System.getProperty("mfa.jdbc.user", "sa"))
                .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD,
                        System.getProperty("mfa.jdbc.password", ""))
                .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER,
                        System.getProperty("mfa.jdbc.driver", "org.h2.Driver"))
                .applySetting(AvailableSettings.HBM2DDL_AUTO,
                        System.getProperty("mfa.jdbc.url") == null ? "create-drop" : "none")
        def dialect = System.getProperty("mfa.dialect")
        if (dialect != null) {
            builder.applySetting(AvailableSettings.DIALECT, dialect)
        }
        def registry = builder.build()
        emf = new MetadataSources(registry)
                .addAnnotatedClass(SecondFactor)
                .addAnnotatedClass(RecoveryCode)
                .addAnnotatedClass(MfaGrace)
                .buildMetadata()
                .buildSessionFactory()
    }

    def cleanupSpec() {
        emf?.close()
    }

    def setup() {
        // Every spec starts from empty tables: order between SPOCK features is not part of the
        // contract, and a leftover factor would silently turn "enrolment" assertions into "challenge".
        truncate()
    }

    // ---------------------------------------------------------------- helpers

    /** Runs one unit of work in its own transaction, the way the production services will. */
    private <T> T inTransaction(Closure<T> work) {
        EntityManager em = emf.createEntityManager()
        def tx = em.transaction
        tx.begin()
        try {
            def result = work.call(em)
            tx.commit()
            return result
        } catch (Throwable t) {
            if (tx.active) tx.rollback()
            throw t
        } finally {
            em.close()
        }
    }

    private void truncate() {
        inTransaction { EntityManager em ->
            em.createNativeQuery("DELETE FROM user_second_factor").executeUpdate()
            em.createNativeQuery("DELETE FROM user_recovery_code").executeUpdate()
            em.createNativeQuery("DELETE FROM user_mfa_grace").executeUpdate()
        }
    }

    private static SecondFactorRepository factors(EntityManager em) {
        new JpaSecondFactorRepository(em)
    }

    private static RecoveryCodeRepository recoveryCodes(EntityManager em) {
        new JpaRecoveryCodeRepository(em)
    }

    private static MfaGraceRepository graces(EntityManager em) {
        new JpaMfaGraceRepository(em)
    }

    // ---------------------------------------------------------------- 1. factors

    def "a TOTP seed survives the round trip as a protected secret, byte for byte"() {
        when:
        def id = inTransaction { em ->
            def saved = factors(em).save(
                    SecondFactor.enrolTotp(USER, SEED, "iPhone (1Password)", NOW))
            em.flush()
            saved.id()
        }

        then:
        def reloaded = inTransaction { em -> factors(em).findById(id).orElseThrow() }
        reloaded.secret().orElseThrow() == SEED
        reloaded.type() == life.qbic.identity.domain.model.mfa.SecondFactorType.TOTP
        reloaded.displayName() == "iPhone (1Password)"
        reloaded.createdAt() == NOW
        reloaded.isActive()
    }

    def "a WebAuthn credential round-trips and exposes no secret"() {
        given:
        def credential = '{"credentialId":"abc","publicKey":"MFkw...","signCount":0}'

        when:
        def id = inTransaction { em ->
            def saved = factors(em).save(SecondFactor.enrolWebAuthn(USER, credential, "YubiKey 5", NOW))
            em.flush()
            saved.id()
        }

        then:
        def reloaded = inTransaction { em -> factors(em).findById(id).orElseThrow() }
        reloaded.webauthnCredential().orElseThrow() == credential
        reloaded.secret().isEmpty()
    }

    def "a removed factor stops counting as enrolment but stays visible to its owner"() {
        given:
        def id = inTransaction { em ->
            def saved = factors(em).save(SecondFactor.enrolTotp(USER, SEED, "iPhone", NOW))
            em.flush()
            saved.id()
        }

        when:
        inTransaction { em ->
            def factor = factors(em).findById(id).orElseThrow()
            factor.disable(NOW.plusSeconds(60))
            factors(em).save(factor)
        }

        then: "AUTH-R-02: enrolment means an ACTIVE factor — the login path must see false"
        !inTransaction { em -> factors(em).hasActiveFactor(USER) }

        and: "the settings surface still shows the row, so 'removed' is distinguishable from 'never existed'"
        def all = inTransaction { em -> factors(em).findAllByUserId(USER) }
        all.size() == 1
        all.first().disabledAt() == NOW.plusSeconds(60)

        and:
        inTransaction { em -> factors(em).findActiveByUserId(USER) }.isEmpty()
    }

    def "a second factor of another user is not enrolment for this one"() {
        given:
        inTransaction { em ->
            factors(em).save(SecondFactor.enrolTotp("u-2", SEED, "iPhone", NOW))
            em.flush()
        }

        expect:
        !inTransaction { em -> factors(em).hasActiveFactor(USER) }
    }

    // ---------------------------------------------------------------- 2. recovery codes

    def "a recovery code is spent exactly once, and spending it is durable"() {
        given: "a generated set of three"
        def hashes = (1..3).collect { "hash-$it" }
        inTransaction { em ->
            recoveryCodes(em).saveAll(hashes.collect { RecoveryCode.issue(USER, it, NOW) })
            em.flush()
        }

        expect:
        inTransaction { em -> recoveryCodes(em).countUsableByUserId(USER) } == 3

        when: "the user presents one"
        def consumed = inTransaction { em ->
            recoveryCodes(em).consumeIfUsable("hash-2", NOW.plusSeconds(30))
        }

        then: "resolved by hash alone — the account is what the user is trying to reach"
        consumed.isPresent()
        consumed.get().userId() == USER
        consumed.get().usedAt() == NOW.plusSeconds(30)

        and: "the same code never works again"
        inTransaction { em -> recoveryCodes(em).consumeIfUsable("hash-2", NOW.plusSeconds(60)) }
                .isEmpty()

        and: "the other two are untouched"
        inTransaction { em -> recoveryCodes(em).countUsableByUserId(USER) } == 2
    }

    def "regenerating a set retires every unused code and leaves spent ones marked as used"() {
        given:
        inTransaction { em ->
            recoveryCodes(em).saveAll((1..3).collect { RecoveryCode.issue(USER, "hash-$it", NOW) })
            em.flush()
        }
        inTransaction { em -> recoveryCodes(em).consumeIfUsable("hash-1", NOW.plusSeconds(10)) }

        when:
        def retired = inTransaction { em -> recoveryCodes(em).invalidateAllFor(USER, NOW.plus(Duration.ofDays(1))) }

        then: "only the two unused codes were retired"
        retired == 2
        inTransaction { em -> recoveryCodes(em).countUsableByUserId(USER) } == 0

        and: "the used code keeps its consumption time, so the audit trail says 'used', not 'retired'"
        def all = inTransaction { em -> recoveryCodes(em).findAllByUserId(USER) }
        def used = all.find { it.codeHash() == "hash-1" }
        used.usedAt() == NOW.plusSeconds(10)
        used.invalidatedAt() == null
        all.findAll { it.codeHash() != "hash-1" }.every { it.invalidatedAt() == NOW.plus(Duration.ofDays(1)) }
    }

    def "exactly one of N concurrent submissions of the same recovery code wins"() {
        given: "one code, eight simultaneous submissions"
        inTransaction { em ->
            recoveryCodes(em).saveAll([RecoveryCode.issue(USER, "hash-1", NOW)])
            em.flush()
        }
        int threads = 8
        def start = new CountDownLatch(1)
        def pool = Executors.newFixedThreadPool(threads)

        when:
        def futures = (1..threads).collect {
            pool.submit({
                start.await()
                inTransaction { em -> recoveryCodes(em).consumeIfUsable("hash-1", NOW.plusSeconds(5)) }
            } as Callable<Optional<RecoveryCode>>)
        }
        start.countDown()
        def results = futures.collect { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        then: "one code must never buy two sessions — row locking, not the unique index, decides this"
        results.count { it.isPresent() } == 1
        inTransaction { em -> recoveryCodes(em).countUsableByUserId(USER) } == 0
    }

    // ---------------------------------------------------------------- 3. grace window

    def "a grace window is written once and never moved by a later login"() {
        given: "the account's first login under REQUIRED"
        def granted = MfaGate.decide(MfaPolicyMode.REQUIRED, false, Optional.empty(), NOW, TWO_DAYS)
                as MfaGateDecision.GraceGranted

        and: "it is persisted"
        inTransaction { em ->
            graces(em).startIfAbsent(MfaGrace.start(USER, NOW, TWO_DAYS))
        }

        when: "the user logs in again a day later, with a fresh decision that would compute a later deadline"
        def later = NOW.plus(Duration.ofDays(1))
        def stored = inTransaction { em ->
            graces(em).startIfAbsent(MfaGrace.start(USER, later, TWO_DAYS))
        }

        then: "the first deadline stands — grace cannot be renewed by logging in"
        stored.graceUntil() == granted.graceUntil()
        stored.startedAt() == NOW
    }

    def "the gate can decide again from stored state, and the second login is inside the window"() {
        given: "a granted window was persisted"
        inTransaction { em -> graces(em).startIfAbsent(MfaGrace.start(USER, NOW, TWO_DAYS)) }

        when: "the account logs in one day later"
        def grace = inTransaction { em -> graces(em).findByUserId(USER) }
        def decision = MfaGate.decide(MfaPolicyMode.REQUIRED, false, grace, NOW.plus(Duration.ofDays(1)), TWO_DAYS)

        then: "still in the window, and no new deadline is granted"
        decision instanceof MfaGateDecision.GraceActive
        (decision as MfaGateDecision.GraceActive).graceUntil() == NOW.plus(TWO_DAYS)
    }

    def "startIfAbsent still reads back the row when the same transaction looked first and found nothing"() {
        // Regression guard: find() may remember an "absent" result inside one persistence context,
        // so the adapter reads back through a query instead of find().
        when:
        def stored = inTransaction { em ->
            def repo = graces(em)
            def before = repo.findByUserId(USER)
            def written = repo.startIfAbsent(MfaGrace.start(USER, NOW, TWO_DAYS))
            [before, written] as List
        }

        then:
        stored[0].isEmpty()
        stored[1].graceUntil() == NOW.plus(TWO_DAYS)
    }

    def "an account that was never subject to REQUIRED has no window, which means 'grant one'"() {
        expect: "absence is not 'grace is over' — the gate turns it into GraceGranted (ADR 0011 item 3)"
        inTransaction { em -> graces(em).findByUserId(USER) }.isEmpty()
        MfaGate.decide(MfaPolicyMode.REQUIRED, false, Optional.empty(), NOW, TWO_DAYS)
                instanceof MfaGateDecision.GraceGranted
    }
}
