package life.qbic.identity.application.mfa

import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.model.mfa.Totp
import life.qbic.identity.domain.repository.SecondFactorRepository
import spock.lang.Specification

import java.time.Instant

/**
 * <b>Accepting a factor is the whole point (AUTH-R-01, AUTH-NFR-02).</b>
 *
 * <p>Three things are pinned: a code only verifies against a factor whose seed produced it, a use is
 * recorded on exactly that factor, and a seed that cannot be decrypted fails <em>closed</em> — the
 * one outcome that must never be mistaken for "no factor enrolled".</p>
 */
class SecondFactorVerificationServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-18T09:00:00Z")

    FakeProtector protector = new FakeProtector()
    FakeRepository repository = new FakeRepository()
    SecondFactorVerificationService service =
            new SecondFactorVerificationService(repository, protector)

    // ---------------------------------------------------------------- the happy path

    def "a valid code verifies and records the use on that factor"() {
        given:
        def factor = enrol("u-1", "iPhone")
        def seed = protector.seedOf(factor)

        when:
        def result = service.verify("u-1", Totp.code(seed, NOW), NOW)

        then:
        result.verified()
        result.outcome() == SecondFactorVerificationService.Outcome.VERIFIED

        and: "the factor itself is updated, so the settings page can say 'last used'"
        factor.lastUsedAt() == NOW
        repository.saved.size() == 1
    }

    def "one step of clock skew is accepted, two are not"() {
        given:
        def factor = enrol("u-1", "iPhone")
        def seed = protector.seedOf(factor)

        expect:
        service.verify("u-1", Totp.code(seed, NOW.plus(java.time.Duration.ofSeconds(30 * offset))), NOW)
                .verified() == accepted

        where:
        offset | accepted
        -1L    | true
        0L     | true
        1L     | true
        2L     | false
    }

    // ---------------------------------------------------------------- the refusing paths

    def "a wrong code is rejected and nothing is recorded"() {
        given:
        def factor = enrol("u-1", "iPhone")
        def staleCode = Totp.code(protector.seedOf(factor), NOW.minus(java.time.Duration.ofMinutes(10)))

        when:
        def result = service.verify("u-1", staleCode, NOW)

        then:
        result.outcome() == SecondFactorVerificationService.Outcome.REJECTED
        factor.lastUsedAt() == null
        repository.saved.isEmpty()
    }

    def "an account without an active factor cannot pass"() {
        when:
        def result = service.verify("u-1", "123456", NOW)

        then:
        result.outcome() == SecondFactorVerificationService.Outcome.NO_FACTOR
    }

    def "a disabled factor does not count, even if its code is right"() {
        given:
        def factor = enrol("u-1", "iPhone")
        def code = Totp.code(protector.seedOf(factor), NOW)
        factor.disable(NOW.minusSeconds(1))

        expect:
        service.verify("u-1", code, NOW).outcome() ==
                SecondFactorVerificationService.Outcome.NO_FACTOR
    }

    def "a seed that cannot be decrypted fails closed, not open"() {
        given:
        enrol("u-1", "iPhone")
        protector.unavailable = true

        when:
        def result = service.verify("u-1", "123456", NOW)

        then: "UNAVAILABLE, never NO_FACTOR — the caller must deny (AUTH-NFR-02)"
        result.outcome() == SecondFactorVerificationService.Outcome.UNAVAILABLE
        !result.verified()
    }

    // ---------------------------------------------------------------- several factors

    def "the code of the second factor verifies against that factor, not the first"() {
        given: "a phone and a tablet"
        def phone = enrol("u-1", "iPhone")
        def tablet = enrol("u-1", "iPad")
        def tabletCode = Totp.code(protector.seedOf(tablet), NOW)

        when:
        def result = service.verify("u-1", tabletCode, NOW)

        then:
        result.verified()
        tablet.lastUsedAt() == NOW
        phone.lastUsedAt() == null
    }

    def "a factor of another account never verifies"() {
        given:
        def foreign = enrol("u-2", "iPhone")

        expect:
        service.verify("u-1", Totp.code(protector.seedOf(foreign), NOW), NOW)
                .outcome() == SecondFactorVerificationService.Outcome.NO_FACTOR
    }

    def "a null argument is refused"() {
        when:
        service.verify(null, "123456", NOW)

        then:
        thrown(NullPointerException)

        when:
        service.verify("u-1", null, NOW)

        then:
        thrown(NullPointerException)

        when:
        service.verify("u-1", "123456", null)

        then:
        thrown(NullPointerException)
    }

    // ---------------------------------------------------------------- helpers

    private SecondFactor enrol(String userId, String name) {
        def seed = Totp.newSeed()
        def factor = SecondFactor.enrolTotp(userId,
                ProtectedSecret.ofBase64(protector.protect(seed.toCharArray())), name, NOW)
        return repository.insert(factor)
    }

    /** Protector that keeps the plaintext so the test can compute real codes. */
    static class FakeProtector implements SecondFactorSecretProtector {
        final Map<String, String> seeds = [:]
        boolean unavailable

        @Override
        String protect(char[] plaintext) {
            def seed = new String(plaintext)
            // 32 seed chars + filler clears ProtectedSecret's nonce+tag floor.
            def blob = Base64.encoder.encodeToString((seed + "0" * 16).getBytes("UTF-8"))
            seeds[blob] = seed
            return blob
        }

        @Override
        char[] reveal(String protectedSecret) {
            if (unavailable) {
                throw new SecondFactorSecretUnavailableException("vault key unavailable")
            }
            def seed = seeds[protectedSecret]
            if (seed == null) {
                throw new SecondFactorSecretUnavailableException("unknown ciphertext")
            }
            return seed.toCharArray()
        }

        String seedOf(SecondFactor factor) {
            seeds[factor.secret().orElseThrow().encoded()]
        }
    }

    static class FakeRepository implements SecondFactorRepository {
        final List<SecondFactor> stored = []
        final List<SecondFactor> saved = []

        /** Test setup: puts a factor in the store without counting as a verification save. */
        SecondFactor insert(SecondFactor factor) {
            stored << factor
            return factor
        }

        @Override
        SecondFactor save(SecondFactor factor) {
            saved << factor
            if (!stored.any { it.is(factor) }) {
                stored << factor
            }
            return factor
        }

        @Override
        Optional<SecondFactor> findById(long id) {
            Optional.empty()
        }

        @Override
        List<SecondFactor> findAllByUserId(String userId) {
            stored.findAll { it.userId() == userId }
        }

        @Override
        List<SecondFactor> findActiveByUserId(String userId) {
            stored.findAll { it.userId() == userId && it.isActive() }
        }

        @Override
        boolean hasActiveFactor(String userId) {
            !findActiveByUserId(userId).isEmpty()
        }
    }
}
