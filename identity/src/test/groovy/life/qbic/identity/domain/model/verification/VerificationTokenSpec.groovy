package life.qbic.identity.domain.model.verification

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>The aggregate that decides whether a verification link may be used (AUTH-R-05).</b>
 *
 * <p>{@link VerificationToken#checkConsumable} is the single source of truth for every gating rule —
 * purpose, already-used, superseded, expired. The persistence adapter calls it inside the
 * consumption transaction and does not re-implement anything, so these assertions are the actual
 * security rules, not a convenience layer.</p>
 */
class VerificationTokenSpec extends Specification {

    private static final Instant ISSUED = Instant.parse("2026-11-10T10:00:00Z")
    private static final Duration HOUR = Duration.ofHours(1)
    private static final String HASH = "a" * 64

    private static VerificationToken aToken() {
        VerificationToken.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, HASH, ISSUED, HOUR)
    }

    // ---------------------------------------------------------------- construction

    def "issuing records the expiry as issue time plus lifetime"() {
        when:
        def token = aToken()

        then:
        token.createdAt() == ISSUED
        token.expiresAt() == ISSUED.plus(HOUR)
        token.userId() == "u-1"
        token.purpose() == TokenPurpose.EMAIL_CONFIRMATION
        token.tokenHash() == HASH
    }

    def "a freshly issued token is neither consumed nor superseded"() {
        when:
        def token = aToken()

        then:
        !token.isConsumed()
        !token.isSuperseded()
        token.consumedAt() == null
        token.supersededAt() == null
    }

    @Unroll
    def "a lifetime of #lifetime is rejected"() {
        when:
        VerificationToken.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, HASH, ISSUED, lifetime)

        then:
        thrown(IllegalArgumentException)

        where:
        lifetime << [Duration.ZERO, Duration.ofSeconds(-1), Duration.ofHours(-1)]
    }

    @Unroll
    def "a hash of '#hash' is rejected"() {
        when:
        VerificationToken.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, hash, ISSUED, HOUR)

        then:
        thrown(IllegalArgumentException)

        where:
        hash << [
                "too-short",
                "A" * 64,                                   // upper case is not accepted
                "g" * 64,                                   // not hex
                VerificationTokens.newRawToken(),           // a raw token is not a hash
                "a" * 63,
                "a" * 65,
        ]
    }

    // ---------------------------------------------------------------- expiry

    def "a token is not expired before its expiry instant"() {
        expect:
        !aToken().isExpiredAt(ISSUED.plus(HOUR).minusSeconds(1))
    }

    def "a token is expired exactly at its expiry instant"() {
        expect: "the boundary is inclusive — the bound in AUTH-R-05 must not be exceeded"
        aToken().isExpiredAt(ISSUED.plus(HOUR))
    }

    def "a token is expired after its expiry instant"() {
        expect:
        aToken().isExpiredAt(ISSUED.plus(HOUR).plusSeconds(1))
    }

    // ---------------------------------------------------------------- consumption rules

    def "a fresh token is consumable for its own purpose"() {
        expect:
        aToken().checkConsumable(TokenPurpose.EMAIL_CONFIRMATION, ISSUED.plusSeconds(1)).isEmpty()
        aToken().isUsableAt(ISSUED.plusSeconds(1))
    }

    def "consuming marks the token used and sets the instant"() {
        given:
        def token = aToken()
        def when1 = ISSUED.plusSeconds(60)

        when:
        token.consume(when1)

        then:
        token.isConsumed()
        token.consumedAt() == when1
        !token.isUsableAt(when1.plusSeconds(1))
    }

    def "an already consumed token is refused as ALREADY_CONSUMED"() {
        given:
        def token = aToken()
        token.consume(ISSUED.plusSeconds(10))

        expect:
        token.checkConsumable(TokenPurpose.EMAIL_CONFIRMATION, ISSUED.plusSeconds(20))
                .get() == ConsumeFailure.ALREADY_CONSUMED
    }

    def "a superseded token is refused as SUPERSEDED"() {
        given:
        def token = aToken()
        token.supersede(ISSUED.plusSeconds(10))

        expect:
        token.isSuperseded()
        token.supersededAt() == ISSUED.plusSeconds(10)
        token.checkConsumable(TokenPurpose.EMAIL_CONFIRMATION, ISSUED.plusSeconds(20))
                .get() == ConsumeFailure.SUPERSEDED
    }

    def "superseding twice keeps the first instant, so a retry does not rewrite the audit trail"() {
        given:
        def token = aToken()
        def first = ISSUED.plusSeconds(10)

        when:
        token.supersede(first)
        token.supersede(ISSUED.plusSeconds(99))

        then:
        token.supersededAt() == first
    }

    def "an expired token is refused as EXPIRED"() {
        expect:
        aToken().checkConsumable(TokenPurpose.EMAIL_CONFIRMATION, ISSUED.plus(HOUR))
                .get() == ConsumeFailure.EXPIRED
    }

    def "a token presented for a different purpose is refused as WRONG_PURPOSE"() {
        expect: "this is what stops a confirmation link resetting a password"
        aToken().checkConsumable(TokenPurpose.PASSWORD_RESET, ISSUED.plusSeconds(1))
                .get() == ConsumeFailure.WRONG_PURPOSE
    }

    def "a wrong-purpose attempt is checked BEFORE the consumed or expired state"() {
        given: "a consumed and long-expired token"
        def token = aToken()
        token.consume(ISSUED.plusSeconds(1))

        expect: "purpose is reported first, because it is the more alarming condition to log"
        token.checkConsumable(TokenPurpose.PASSWORD_RESET, ISSUED.plus(HOUR).plusSeconds(1))
                .get() == ConsumeFailure.WRONG_PURPOSE
    }

    def "consuming a token that is no longer usable throws rather than silently no-opping"() {
        given:
        def token = aToken()
        token.consume(ISSUED.plusSeconds(1))

        when:
        token.consume(ISSUED.plusSeconds(2))

        then: "a caller that bypassed checkConsumable() must fail loudly"
        thrown(IllegalStateException)
    }

    def "a wrong-purpose attempt does NOT burn the token"() {
        given:
        def token = aToken()

        when: "presented at the wrong endpoint"
        def refusal = token.checkConsumable(TokenPurpose.PASSWORD_RESET, ISSUED.plusSeconds(1))

        then: "nothing was mutated, so the real owner can still use the link"
        refusal.isPresent()
        !token.isConsumed()
        token.isUsableAt(ISSUED.plusSeconds(2))
    }

    // ---------------------------------------------------------------- identity

    def "equality is by token hash, not by object identity"() {
        given:
        def a = aToken()
        def b = aToken()

        expect: "two instances of the same token are equal; the hash is unique in the schema"
        a == b
        a.hashCode() == b.hashCode()

        and:
        def different = VerificationToken.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION,
                "b" * 64, ISSUED, HOUR)
        a != different
    }

    // ---------------------------------------------------------------- hashing

    def "hashing is deterministic and produces a 64-char lower-case hex digest"() {
        given:
        def raw = "a-fixed-raw-token"

        expect:
        VerificationTokens.hash(raw) == VerificationTokens.hash(raw)
        VerificationTokens.hash(raw) ==~ /[0-9a-f]{64}/
    }

    def "different raw tokens hash differently"() {
        expect: "the unique index on tokenHash depends on this"
        VerificationTokens.hash("a") != VerificationTokens.hash("b")
    }

    def "raw tokens carry 256 bits and are URL-safe"() {
        when:
        def raw = VerificationTokens.newRawToken()

        then: "32 bytes Base64URL without padding — no characters that need percent-encoding"
        raw ==~ /[A-Za-z0-9_-]+/
        raw.length() >= 43          // ceil(32 * 8 / 6)
        raw.length() <= 44

        and:
        VerificationTokens.newRawToken() != raw
    }

    def "hashing rejects null rather than producing a shared hash"() {
        when:
        VerificationTokens.hash(null)

        then:
        thrown(NullPointerException)
    }
}
