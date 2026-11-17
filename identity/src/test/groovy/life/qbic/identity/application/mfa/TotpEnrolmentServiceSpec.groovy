package life.qbic.identity.application.mfa

import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.model.mfa.SecondFactorType
import life.qbic.identity.domain.model.mfa.Totp
import life.qbic.identity.domain.repository.SecondFactorRepository
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Enrolment proves the factor before it exists (AUTH-R-02, AUTH-NFR-01).</b>
 *
 * <p>Two properties are the point of these tests: the pending enrolment carries no plaintext seed, and
 * a confirmation only creates a factor when a code derived from <em>that</em> seed verifies.</p>
 *
 * <p>The protector is faked, but the TOTP algorithm is real — a fake verifier would make the
 * interesting assertion (wrong seed ⇒ no factor) untestable.</p>
 */
class TotpEnrolmentServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z")
    private static final String USER = "u-1"

    FakeProtector protector
    FakeRepository repository
    TotpEnrolmentService service

    def setup() {
        protector = new FakeProtector()
        repository = new FakeRepository()
        service = new TotpEnrolmentService(repository, protector)
    }

    // ---------------------------------------------------------------- begin

    def "begin returns an encrypted seed and a provisioning URI, and persists nothing"() {
        when:
        def pending = service.begin(USER, "iPhone (1Password)", "ada@example.org", NOW)

        then: "half-enrolled is not a state the store has to understand"
        repository.saved.isEmpty()

        and: "the pending carries the ciphertext, not the seed"
        pending.userId() == USER
        pending.displayName() == "iPhone (1Password)"
        pending.createdAt() == NOW
        pending.protectedSeed() ==~ /[A-Za-z0-9+\/=]+/
        !pending.protectedSeed().contains(protector.lastSeed)

        and: "the URI is the only place the plaintext seed appears, because the app must scan it"
        pending.provisioningUri().contains(protector.lastSeed)
        pending.provisioningUri().startsWith("otpauth://totp/")
    }

    def "begin issues a different seed every time"() {
        when:
        def first = service.begin(USER, "iPhone", "ada@example.org", NOW)
        def second = service.begin(USER, "iPhone", "ada@example.org", NOW)

        then:
        first.protectedSeed() != second.protectedSeed()
    }

    def "the pending enrolment does not print its seed material"() {
        when:
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)

        then:
        !pending.toString().contains(pending.protectedSeed())
        !pending.toString().contains(protector.lastSeed)
    }

    // ---------------------------------------------------------------- confirm

    def "a correct code creates an active TOTP factor holding the protected seed"() {
        given:
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)
        def code = Totp.code(protector.plainSeedOf(pending.protectedSeed()), NOW.plusSeconds(5))

        when:
        def factor = service.confirm(pending, code, NOW.plusSeconds(5))

        then: "the factor is proven before it exists"
        factor.type() == SecondFactorType.TOTP
        factor.userId() == USER
        factor.displayName() == "iPhone"
        factor.isActive()
        repository.saved.size() == 1

        and: "what was stored is the ciphertext — the plaintext seed never reaches the repository"
        factor.secret().orElseThrow() == ProtectedSecret.ofBase64(pending.protectedSeed())
        !factor.secret().orElseThrow().encoded().contains(protector.lastSeed)
    }

    def "the code is accepted one step either side of the server clock"() {
        given:
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)
        def seed = protector.plainSeedOf(pending.protectedSeed())
        def code = Totp.code(seed, NOW.plus(Duration.ofSeconds(30 * offset)))

        when: "the user's phone is a step out"
        service.confirm(pending, code, NOW)

        then:
        repository.saved.size() == 1

        where:
        offset << [-1L, 0L, 1L]
    }

    def "a wrong code creates no factor at all"() {
        given: "a code that is valid for a different time, so the mismatch is deterministic"
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)
        def seed = protector.plainSeedOf(pending.protectedSeed())
        def codeFromTenMinutesAgo = Totp.code(seed, NOW.minus(Duration.ofMinutes(10)))

        when:
        service.confirm(pending, codeFromTenMinutesAgo, NOW)

        then: "a factor that never verified must not exist afterwards, not even disabled"
        thrown(InvalidTotpCodeException)
        repository.saved.isEmpty()
    }

    def "a code derived from a different seed is refused"() {
        given: "the user scanned a different enrolment's QR code"
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)
        def otherSeed = Totp.newSeed()

        when:
        service.confirm(pending, Totp.code(otherSeed, NOW), NOW)

        then:
        thrown(InvalidTotpCodeException)
        repository.saved.isEmpty()
    }

    def "a code past the tolerated skew is refused"() {
        given:
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)
        def seed = protector.plainSeedOf(pending.protectedSeed())
        def staleCode = Totp.code(seed, NOW.minus(Duration.ofSeconds(90)))

        when:
        service.confirm(pending, staleCode, NOW)

        then:
        thrown(InvalidTotpCodeException)
        repository.saved.isEmpty()
    }

    def "an abandoned enrolment cannot be confirmed later"() {
        given:
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)
        def code = Totp.code(protector.plainSeedOf(pending.protectedSeed()),
                NOW.plus(TotpEnrolmentService.ENROLMENT_WINDOW).plusSeconds(1))

        when: "the tab was left open for longer than the window"
        service.confirm(pending, code, NOW.plus(TotpEnrolmentService.ENROLMENT_WINDOW).plusSeconds(1))

        then:
        thrown(EnrolmentExpiredException)
        repository.saved.isEmpty()
    }

    def "an enrolment confirmed inside the window still works near its end"() {
        given:
        def justInside = NOW.plus(TotpEnrolmentService.ENROLMENT_WINDOW).minusSeconds(1)
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)
        def code = Totp.code(protector.plainSeedOf(pending.protectedSeed()), justInside)

        expect:
        service.confirm(pending, code, justInside).isActive()
    }

    def "a seed that cannot be decrypted is a fail-closed error, not a failed code"() {
        given:
        def pending = service.begin(USER, "iPhone", "ada@example.org", NOW)
        protector.unavailable = true

        when:
        service.confirm(pending, "123456", NOW)

        then: "the caller must deny, never treat the factor as absent (AUTH-NFR-02)"
        thrown(SecondFactorSecretUnavailableException)
        repository.saved.isEmpty()
    }

    // ---------------------------------------------------------------- fakes

    /**
     * A protector whose output is still valid Base64-shaped ciphertext (so {@code ProtectedSecret}
     * accepts it) but which keeps a map back to the plaintext, so the test can compute real codes.
     */
    static class FakeProtector implements SecondFactorSecretProtector {

        private final Map<String, String> plaintexts = [:]

        String lastSeed
        boolean unavailable

        @Override
        String protect(char[] plaintext) {
            def seed = new String(plaintext)
            lastSeed = seed
            // 32 seed chars + 16 filler bytes clears ProtectedSecret's nonce+tag floor.
            def blob = Base64.encoder.encodeToString((seed + "0" * 16).getBytes("UTF-8"))
            plaintexts[blob] = seed
            return blob
        }

        @Override
        char[] reveal(String protectedSecret) {
            if (unavailable) {
                throw new SecondFactorSecretUnavailableException("vault key unavailable")
            }
            def seed = plaintexts[protectedSecret]
            if (seed == null) {
                throw new SecondFactorSecretUnavailableException("unknown ciphertext")
            }
            return seed.toCharArray()
        }

        String plainSeedOf(String protectedSecret) {
            plaintexts[protectedSecret]
        }
    }

    static class FakeRepository implements SecondFactorRepository {

        final List<SecondFactor> saved = []

        @Override
        SecondFactor save(SecondFactor factor) {
            saved << factor
            return factor
        }

        @Override
        Optional<SecondFactor> findById(long id) {
            Optional.empty()
        }

        @Override
        List<SecondFactor> findAllByUserId(String userId) {
            saved.findAll { it.userId() == userId }
        }

        @Override
        List<SecondFactor> findActiveByUserId(String userId) {
            saved.findAll { it.userId() == userId && it.isActive() }
        }

        @Override
        boolean hasActiveFactor(String userId) {
            !findActiveByUserId(userId).isEmpty()
        }
    }
}
