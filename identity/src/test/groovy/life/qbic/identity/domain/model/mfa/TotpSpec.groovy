package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

/**
 * <b>The algorithm (RFC 4226 / RFC 6238), against the RFC's own vectors.</b>
 *
 * <p>The seed and codes below are the published test vectors, not values produced by this
 * implementation — a TOTP that only agrees with itself would pass a round-trip test and fail every
 * authenticator app.</p>
 */
class TotpSpec extends Specification {

    /** RFC 4226 Appendix D: the ASCII seed "12345678901234567890", Base32-encoded. */
    private static final String RFC_SEED = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    /** RFC 4226 Appendix D: HOTP values for counters 0–9. TOTP at step k is the same value. */
    private static final List<String> RFC_CODES = [
            "755224", "287082", "359152", "969429", "338314",
            "254676", "287922", "162583", "399871", "520489"
    ]

    private static Instant atStep(long step) {
        Instant.ofEpochSecond(step * Totp.PERIOD_SECONDS)
    }

    // ---------------------------------------------------------------- code derivation

    def "the RFC 4226 values reproduce, which is what every authenticator app must agree with"() {
        expect:
        (0..9).every { step -> Totp.code(RFC_SEED, atStep(step)) == RFC_CODES[step] }
    }

    def "a code is stable inside its 30-second step and changes with the next one"() {
        given:
        def start = atStep(7)

        expect:
        Totp.code(RFC_SEED, start) == RFC_CODES[7]
        Totp.code(RFC_SEED, start.plusSeconds(29)) == RFC_CODES[7]
        Totp.code(RFC_SEED, start.plusSeconds(30)) == RFC_CODES[8]
    }

    def "codes are six digits, zero-padded"() {
        expect:
        Totp.code(RFC_SEED, atStep(step)).matches("\\d{6}")

        where:
        step << (0..20)
    }

    // ---------------------------------------------------------------- verification

    def "the current code verifies"() {
        expect:
        Totp.verify(RFC_SEED, RFC_CODES[1], atStep(1).plusSeconds(10))
    }

    @Unroll
    def "one step of clock skew is tolerated (#offset)"() {
        given: "the user's phone is a step behind or ahead of the server"
        def now = atStep(5).plusSeconds(12)

        expect: "the neighbouring step's code is accepted, which is what makes a drifting device usable"
        Totp.verify(RFC_SEED, RFC_CODES[5 + offset], now)

        where:
        offset << [-1, 0, 1]
    }

    @Unroll
    def "two steps of skew is not tolerated (#offset)"() {
        given:
        def now = atStep(5).plusSeconds(12)

        expect: "each extra step is extra time in which a relayed code still works"
        !Totp.verify(RFC_SEED, RFC_CODES[5 + offset], now)

        where:
        offset << [-2, 2]
    }

    def "a code from a distant step is refused"() {
        given:
        def now = atStep(5).plusSeconds(12)
        def stale = Totp.code(RFC_SEED, atStep(1))

        expect: "five steps back is a minute and a half — well past any tolerated drift"
        !Totp.verify(RFC_SEED, stale, now)
    }

    def "a code from another seed is refused"() {
        given:
        def otherSeed = Totp.newSeed()

        expect:
        !Totp.verify(otherSeed, RFC_CODES[3], atStep(3))
    }

    @Unroll
    def "malformed input is refused rather than trimmed into shape: '#presented'"() {
        expect:
        !Totp.verify(RFC_SEED, presented, atStep(3))

        where:
        presented << ["", "12345", "1234567", "abcdef", "12 34 56", "12345678", "+12345"]
    }

    def "surrounding whitespace is ignored, because codes get copied from messages"() {
        expect:
        Totp.verify(RFC_SEED, "  287082  ", atStep(1))
    }

    def "an empty seed is a programming error, not a failed verification"() {
        when:
        Totp.code("", atStep(1))

        then:
        thrown(IllegalArgumentException)
    }

    // ---------------------------------------------------------------- seeds

    def "a generated seed is 32 Base32 characters and decodes to 160 bits"() {
        when:
        def seed = Totp.newSeed()

        then:
        seed.length() == 32
        seed ==~ /[A-Z2-7]{32}/
        Base32.decode(seed).length == 20
    }

    def "two seeds are never the same"() {
        expect:
        (1..50).collect { Totp.newSeed() }.unique().size() == 50
    }

    def "a generated seed verifies its own code"() {
        given:
        def seed = Totp.newSeed()
        def now = Instant.parse("2026-11-17T10:00:00Z")

        when:
        def code = Totp.code(seed, now)

        then:
        Totp.verify(seed, code, now)
    }

    def "the step is derived from Unix time, so a negative epoch does not throw"() {
        expect:
        Totp.stepAt(Instant.parse("1969-12-31T23:59:30Z")) == -1
        Totp.stepAt(Instant.EPOCH) == 0
    }
}
