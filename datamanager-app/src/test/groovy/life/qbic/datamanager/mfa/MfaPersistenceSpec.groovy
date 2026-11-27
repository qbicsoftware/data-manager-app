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
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.domain.repository.AuthAttemptCounter
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

    static String ddlAuto() {
        System.getProperty("mfa.jdbc.url") == null ? "create-drop" : "none"
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
                .applySetting(AvailableSettings.HBM2DDL_AUTO, ddlAuto())
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
        if (ddlAuto() == "none") {
            createRealSchema()
        }
    }

    /**
     * Splits a script on {@code ;} while ignoring semicolons inside single-quoted strings.
     *
     * <p>A plain {@code split(";")} is wrong the moment a statement contains a string with
     * punctuation in it — the draft's preflight warning did, and the split produced a statement
     * that only looked executable. The mysql client understands quoting; this harness has to as
     * well, or a MariaDB run measures the harness rather than the DDL.</p>
     */
    private static final char QUOTE = (char) 39
    private static final char SEMICOLON = (char) 59

    private static List<String> splitStatements(String sql) {
        def cleaned = sql.readLines()
                .findAll { !it.trim().startsWith("--") && !it.trim().isEmpty() }
                .join("\n")
        List<String> statements = []
        StringBuilder current = new StringBuilder()
        boolean inString = false
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i)
            if (c == QUOTE) {
                // Two quotes in a row are an escaped quote inside a string, not a delimiter.
                if (inString && i + 1 < cleaned.length() && cleaned.charAt(i + 1) == QUOTE) {
                    current.append("''")
                    i++
                    continue
                }
                inString = !inString
            }
            if (c == SEMICOLON && !inString) {
                statements << current.toString().trim()
                current.setLength(0)
                continue
            }
            current.append(c)
        }
        if (!current.toString().trim().isEmpty()) {
            statements << current.toString().trim()
        }
        return statements.findAll { !it.isEmpty() }
    }

    /**
     * Applies the migration draft's DDL verbatim, so a MariaDB run exercises the signed-off schema —
     * including the {@code users} foreign key, the shape CHECK constraint and the global unique hash —
     * instead of a Hibernate approximation. Those three are exactly what H2 cannot tell us anything
     * about, and exactly what the §12 review has to sign off.
     */
    private void createRealSchema() {
        // surefire runs with the module as working directory, so walk up to the repo root.
        def f = new File("sql/migrations/drafts/mfa-second-factor.sql")
        if (!f.exists()) {
            f = new File("../sql/migrations/drafts/mfa-second-factor.sql")
        }
        def statements = splitStatements(f.text)
        inTransaction { EntityManager em ->
            // The FK target, in the charset/collation the real `users` table uses. Production always
            // has it; this is scaffolding for a throwaway database only.
            em.createNativeQuery("""
                CREATE TABLE IF NOT EXISTS users (
                    id varchar(255) NOT NULL PRIMARY KEY
                ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
                """).executeUpdate()
            statements.each { em.createNativeQuery(it.trim()).executeUpdate() }
        }
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

    /**
     * A second-factor throttle with an allowance wide enough that it never interferes.
     *
     * <p>The recovery-code service now takes the throttle (MFA-68), and these cases are about the
     * store — that a hash redeems once, that a foreign code is refused unspent. A throttle that
     * blocked mid-spec would turn a storage assertion into a rate-limit assertion; the limiter's own
     * behaviour is pinned in {@code RecoveryCodeThrottleSpec}.</p>
     */
    private static AuthAttemptThrottle permissiveSecondFactorThrottle() {
        def neverBlocks = new AuthAttemptCounter() {
            @Override
            AuthAttemptCounter.Counter increment(String scope, String key, Instant now, Duration window) {
                return new AuthAttemptCounter.Counter(1, null)
            }

            @Override
            void block(String scope, String key, Instant blockedUntil) {
                throw new AssertionError("a permissive counter must never block")
            }

            @Override
            AuthAttemptCounter.Counter peek(String scope, String key) {
                return new AuthAttemptCounter.Counter(0, null)
            }

            @Override
            void clear(String scope, String key) {
            }
        }
        new AuthAttemptThrottle(neverBlocks,
                10, Duration.ofMinutes(15), Duration.ofMinutes(15),
                5, Duration.ofMinutes(15), Duration.ofMinutes(15),
                1000, Duration.ofMinutes(15), Duration.ofMinutes(15))
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

    def "a generated recovery-code set works against the real store, exactly once"() {
        given: "the service, the encoder and the JPA adapter wired as production will wire them"
        def encoder = new life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder(
                "p".repeat(life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder.MIN_PEPPER_LENGTH))
        def throttle = permissiveSecondFactorThrottle()

        when: "a set is generated"
        def set = inTransaction { em ->
            new life.qbic.identity.application.mfa.RecoveryCodeService(
                    recoveryCodes(em), encoder, throttle).generate(USER, NOW)
        }

        then: "only hashes reached the store"
        inTransaction { em -> recoveryCodes(em).countUsableByUserId(USER) } ==
                life.qbic.identity.domain.model.mfa.RecoveryCodes.SET_SIZE

        and: "a printed code redeems by hash alone, without the account"
        inTransaction { em ->
            new life.qbic.identity.application.mfa.RecoveryCodeService(recoveryCodes(em), encoder,
                    throttle).redeem(set.formattedCodes()[0].toLowerCase(), NOW.plusSeconds(60))
        }

        and: "and never again"
        !inTransaction { em ->
            new life.qbic.identity.application.mfa.RecoveryCodeService(recoveryCodes(em), encoder,
                    throttle).redeem(set.codes()[0], NOW.plusSeconds(120))
        }

        and: "the count reflects it"
        inTransaction { em -> recoveryCodes(em).countUsableByUserId(USER) } ==
                life.qbic.identity.domain.model.mfa.RecoveryCodes.SET_SIZE - 1
    }

    def "a recovery code presented for another account is refused without being spent"() {
        given: "a set generated for u-2, and the hash of its first code"
        def encoder = new life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder(
                "p".repeat(life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder.MIN_PEPPER_LENGTH))
        def set = inTransaction { em ->
            new life.qbic.identity.application.mfa.RecoveryCodeService(
                    recoveryCodes(em), encoder, permissiveSecondFactorThrottle()).generate("u-2", NOW)
        }
        def hash = encoder.hash(set.codes()[0])

        when: "u-1 is mid-login and presents it"
        def foreign = inTransaction { em ->
            recoveryCodes(em).consumeIfUsableForUser(hash, USER, NOW.plusSeconds(30))
        }

        then: "refused, and the owner's code is still there"
        foreign.isEmpty()
        inTransaction { em -> recoveryCodes(em).countUsableByUserId("u-2") } ==
                life.qbic.identity.domain.model.mfa.RecoveryCodes.SET_SIZE

        and: "the legitimate owner can still spend it"
        inTransaction { em ->
            recoveryCodes(em).consumeIfUsableForUser(hash, "u-2", NOW.plusSeconds(60))
        }.isPresent()
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

    def "N concurrent first logins all read a window back, and they agree on the deadline"() {
        // A new account under REQUIRED, several logins in flight, all deciding "no window, grant one".
        //
        // Honest scope: this does NOT reproduce the reported failure. It runs on H2, whose default
        // isolation is READ COMMITTED, so a concurrent commit is visible to this transaction's next
        // statement. MariaDB defaults to REPEATABLE READ, where a plain SELECT keeps reading the
        // snapshot taken at the first read - which is how a committed row stayed invisible and made
        // one login return HTTP 500.
        //
        // Forcing REPEATABLE READ on H2 was tried and abandoned: H2's locking reads still read the
        // snapshot (InnoDB's do not), and the global setting broke an unrelated spec inside H2's own
        // FOR UPDATE evaluation. A spec that fails for a reason the engine invented is worse than no
        // spec.
        //
        // So the failure is pinned where it can be stated exactly: JpaMfaGraceRepositorySpec asserts
        // that a successful insert performs NO read at all (so no snapshot can hide the row), and that
        // the only remaining read is a locking one. This test keeps the end-to-end invariant.
        given:
        int threads = 8
        def start = new CountDownLatch(1)
        def pool = Executors.newFixedThreadPool(threads)
        def outcomes = Collections.synchronizedList([])

        when:
        (1..threads).each { index ->
            pool.submit({
                start.await()
                try {
                    def read = inTransaction { em ->
                        def repo = graces(em)
                        repo.findByUserId(USER).isEmpty() ? repo.startIfAbsent(
                                MfaGrace.start(USER, NOW.plus(Duration.ofSeconds(index)), TWO_DAYS))
                                : repo.findByUserId(USER).get()
                    }
                    outcomes << read
                } catch (Throwable t) {
                    outcomes << t
                }
            } as Callable)
        }
        start.countDown()
        pool.shutdown()
        pool.awaitTermination(30, TimeUnit.SECONDS)

        then: "every login got a window back - none is denied because its read-back lost a race"
        outcomes.size() == threads
        outcomes.findAll { it instanceof Throwable }*.toString() == []

        and: "each one is a real window, not the candidate of a login that gave up"
        outcomes.every { it instanceof MfaGrace }

        and: "and they all agree on which deadline is stored"
        def deadlines = outcomes.findAll { it instanceof MfaGrace }*.graceUntil() as Set
        deadlines.size() == 1
    }

    def "an account that was never subject to REQUIRED has no window, which means 'grant one'"() {
        expect: "absence is not 'grace is over' — the gate turns it into GraceGranted (ADR 0011 item 3)"
        inTransaction { em -> graces(em).findByUserId(USER) }.isEmpty()
        MfaGate.decide(MfaPolicyMode.REQUIRED, false, Optional.empty(), NOW, TWO_DAYS)
                instanceof MfaGateDecision.GraceGranted
    }
}
