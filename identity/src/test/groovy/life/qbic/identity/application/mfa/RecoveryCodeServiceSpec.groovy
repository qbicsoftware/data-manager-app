package life.qbic.identity.application.mfa

import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.domain.model.mfa.RecoveryCode
import life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder
import life.qbic.identity.domain.model.mfa.RecoveryCodes
import life.qbic.identity.domain.repository.AuthAttemptCounter
import life.qbic.identity.domain.repository.RecoveryCodeRepository
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Generate once, redeem once, regenerate retires the old set (AUTH-R-03).</b>
 *
 * <p>The plaintext codes exist exactly once, on the screen after generation. Everything else — the
 * store, the return value after redemption, the log — sees hashes or counts.</p>
 */
class RecoveryCodeServiceSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z")
    private static final String USER = "u-1"

    FakeRepository repository
    RecoveryCodeEncoder encoder
    RecoveryCodeService service

    def setup() {
        repository = new FakeRepository()
        encoder = new RecoveryCodeEncoder("p".repeat(RecoveryCodeEncoder.MIN_PEPPER_LENGTH))
        // Generous allowance: these cases are about generation and redemption semantics, and the
        // throttle has its own spec (RecoveryCodeThrottleSpec).
        service = new RecoveryCodeService(repository, encoder, new AuthAttemptThrottle(
                new PermissiveCounter(), 10, Duration.ofMinutes(15), Duration.ofMinutes(15),
                5, Duration.ofMinutes(15), Duration.ofMinutes(15),
                1000, Duration.ofMinutes(15), Duration.ofMinutes(15)))
    }

    // ---------------------------------------------------------------- generation

    def "generating returns ten codes and persists only their hashes"() {
        when:
        def set = service.generate(USER, NOW)

        then:
        set.userId() == USER
        set.codes().size() == RecoveryCodes.SET_SIZE
        set.formattedCodes().every { it ==~ /[A-Z2-7]{4}-[A-Z2-7]{4}-[A-Z2-7]{4}/ }

        and: "the database holds hashes, never the codes themselves"
        repository.codes.size() == RecoveryCodes.SET_SIZE
        set.codes().every { code ->
            repository.codes.every { stored ->
                stored.codeHash() != code && !stored.codeHash().toUpperCase().contains(code)
            }
        }
        repository.codes.every { it.codeHash() ==~ /[0-9a-f]{64}/ }
        repository.codes.every { it.userId() == USER }
    }

    def "the generated set is usable: every returned code redeems exactly once"() {
        given:
        def set = service.generate(USER, NOW)

        expect:
        set.codes().every { service.redeem(it, NOW.plusSeconds(60)) }

        and: "and none of them works again"
        set.codes().every { !service.redeem(it, NOW.plusSeconds(120)) }
    }

    def "regenerating retires every unused code of the previous set"() {
        given:
        def first = service.generate(USER, NOW)
        service.redeem(first.codes()[0], NOW.plusSeconds(10))

        when:
        def second = service.generate(USER, NOW.plus(Duration.ofDays(1)))

        then: "exactly one live set: the user cannot keep two by generating again"
        repository.usable(USER) == RecoveryCodes.SET_SIZE
        !service.redeem(first.codes()[1], NOW.plus(Duration.ofDays(1)).plusSeconds(5))
        second.codes().every { service.redeem(it, NOW.plus(Duration.ofDays(1)).plusSeconds(5)) }

        and: "the code that was already spent keeps 'used', not 'retired'"
        repository.codes.find { it.codeHash() == encoder.hash(first.codes()[0]) }.usedAt() != null
        repository.codes.find { it.codeHash() == encoder.hash(first.codes()[1]) }.usedAt() == null
    }

    // ---------------------------------------------------------------- redemption

    def "an unknown code is refused without saying why"() {
        given:
        service.generate(USER, NOW)

        expect: "unknown, already used and retired must all look the same (AUTH-NFR-03)"
        !service.redeem("A2B3C4D5E6F7", NOW)
    }

    def "a malformed code is a failed attempt, not an exception on the login path"() {
        expect:
        !service.redeem("nonsense", NOW)
        !service.redeem("A2B3C4D5E6F", NOW)
        !service.redeem("A2B3C4D5E6F0", NOW)
    }

    def "a code typed with hyphens and lower case still redeems"() {
        given:
        def set = service.generate(USER, NOW)

        expect: "the user copied what was printed, not what was stored"
        service.redeem(set.formattedCodes()[0].toLowerCase(), NOW.plusSeconds(30))
    }

    def "remaining counts what is still usable"() {
        given:
        def set = service.generate(USER, NOW)

        expect:
        service.remaining(USER) == RecoveryCodes.SET_SIZE

        when:
        service.redeem(set.codes()[0], NOW.plusSeconds(10))

        then:
        service.remaining(USER) == RecoveryCodes.SET_SIZE - 1
    }

    // ---------------------------------------------------------------- login challenge (M4-3)

    def "a code of the mid-login account redeems and is spent"() {
        given:
        def set = service.generate(USER, NOW)

        expect:
        service.redeemFor(USER, set.codes()[0], NOW.plusSeconds(30)).redeemed()
        service.remaining(USER) == RecoveryCodes.SET_SIZE - 1

        and: "and only once"
        !service.redeemFor(USER, set.codes()[0], NOW.plusSeconds(60)).redeemed()
    }

    def "another account's code is refused AND not spent — the owner may still need it"() {
        given: "codes generated for u-2"
        def other = service.generate("u-2", NOW)

        when: "u-1 is mid-login and presents one of them"
        def accepted = service.redeemFor("u-1", other.codes()[0], NOW.plusSeconds(30)).redeemed()

        then: "refused, and u-2 still has a working set"
        !accepted
        service.remaining("u-2") == RecoveryCodes.SET_SIZE

        and: "the legitimate owner can still use it"
        service.redeemFor("u-2", other.codes()[0], NOW.plusSeconds(60))
    }

    def "a malformed recovery code at the challenge is a failed attempt, not an exception"() {
        expect:
        !service.redeemFor(USER, "nonsense", NOW).redeemed()
        !service.redeemFor(USER, "", NOW).redeemed()
    }

    def "the challenge path requires an account"() {
        when:
        service.redeemFor(null, "A2B3C4D5E6F7", NOW)

        then:
        thrown(NullPointerException)
    }

    // ---------------------------------------------------------------- wiring

    def "every method that consumes a code is transactional"() {
        // The repository takes a PESSIMISTIC_WRITE lock and flushes; without an active transaction
        // Hibernate throws "No active transaction" — at runtime, on the login path, which is how this
        // was found. The fake repository in these specs cannot reproduce it, so assert the annotation.
        expect:
        ["generate", "redeem", "redeemFor"].every { name ->
            def method = RecoveryCodeService.getMethods().find { it.name == name }
            method != null && method.isAnnotationPresent(
                    org.springframework.transaction.annotation.Transactional)
        }
    }

    // ---------------------------------------------------------------- plaintext hygiene

    def "the returned set does not print its codes"() {
        when:
        def set = service.generate(USER, NOW)

        then: "this object is short-lived by design; a log line or session dump is its likely fate"
        set.toString().contains(USER)
        set.codes().every { !set.toString().contains(it) }
    }

    def "generation requires an account and an instant"() {
        when:
        service.generate(" ", NOW)

        then:
        thrown(IllegalArgumentException)

        when:
        service.generate(USER, null)

        then:
        thrown(NullPointerException)
    }

    // ---------------------------------------------------------------- fake

    /** In-memory store with the same semantics the JPA adapter gives: hashes are unique, use is final. */
    /** Never blocks: the cases here are about semantics, not about the rate limit. */
    static class PermissiveCounter implements AuthAttemptCounter {
        @Override
        Counter increment(String scope, String key, Instant now, Duration window) {
            return new Counter(1, null)
        }

        @Override
        void block(String scope, String key, Instant blockedUntil) {
        }

        @Override
        Counter peek(String scope, String key) {
            return new Counter(0, null)
        }

        @Override
        void clear(String scope, String key) {
        }
    }

    static class FakeRepository implements RecoveryCodeRepository {

        final List<RecoveryCode> codes = []

        @Override
        List<RecoveryCode> saveAll(List<RecoveryCode> toStore) {
            // RecoveryCode.issue() has no id; emulate the surrogate key for the fake.
            toStore.each { codes << it }
            return toStore
        }

        @Override
        List<RecoveryCode> findAllByUserId(String userId) {
            codes.findAll { it.userId() == userId }
        }

        @Override
        long countUsableByUserId(String userId) {
            usable(userId)
        }

        @Override
        int invalidateAllFor(String userId, Instant now) {
            int retired = 0
            codes.findAll { it.userId() == userId && it.isUsable() }.each {
                if (it.invalidate(now)) retired++
            }
            retired
        }

        @Override
        Optional<RecoveryCode> consumeIfUsable(String codeHash, Instant now) {
            return consumeFor(codeHash, null, now)
        }

        @Override
        Optional<RecoveryCode> consumeIfUsableForUser(String codeHash, String userId, Instant now) {
            return consumeFor(codeHash, userId, now)
        }

        private Optional<RecoveryCode> consumeFor(String codeHash, String userId, Instant now) {
            def match = codes.find {
                it.codeHash() == codeHash && it.isUsable()
                        && (userId == null || it.userId() == userId)
            }
            if (match == null) {
                return Optional.empty()
            }
            match.consume(now)
            return Optional.of(match)
        }

        long usable(String userId) {
            codes.count { it.userId() == userId && it.isUsable() }
        }
    }
}
