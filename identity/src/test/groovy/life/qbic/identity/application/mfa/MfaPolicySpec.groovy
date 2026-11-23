package life.qbic.identity.application.mfa

import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>The posture switch (ADR 0011 P1).</b>
 *
 * <p>Two properties matter: the default must not silently change behaviour for an installation that
 * has not decided anything, and a typo must fail at startup instead of being read as "off" by someone
 * who believes enforcement is on.</p>
 */
class MfaPolicySpec extends Specification {

    def "the default posture is OFF, so deploying the challenge changes nothing"() {
        when:
        def policy = new MfaPolicy("OFF", MfaPolicy.DEFAULT_GRACE, "")

        then:
        policy.mode() == MfaPolicyMode.OFF
        policy.grace() == Duration.ofHours(48)
    }

    @Unroll
    def "the mode is read case-insensitively and tolerantly: '#value'"() {
        expect:
        new MfaPolicy(value, Duration.ofHours(1), "").mode() == expected

        where:
        value           | expected
        "optional"      | MfaPolicyMode.OPTIONAL
        " REQUIRED "    | MfaPolicyMode.REQUIRED
        "Off"           | MfaPolicyMode.OFF
    }

    @Unroll
    def "an unknown mode fails loudly rather than defaulting to off: '#value'"(String value) {
        when:
        new MfaPolicy(value, Duration.ofHours(1), "")

        then: "a typo must not disable a security control silently"
        def e = thrown(IllegalArgumentException)
        e.message.contains("allowed values")

        where:
        value << ["enabled", "", "REQUIRE", "2"]
    }

    def "a missing mode is refused"() {
        when:
        new MfaPolicy(null, Duration.ofHours(1), "")

        then:
        thrown(NullPointerException)
    }

    @Unroll
    def "a non-positive grace window is refused (#window)"(Duration window) {
        when: "a zero window is a lockout with extra steps"
        new MfaPolicy("REQUIRED", window, "")

        then:
        thrown(IllegalArgumentException)

        where:
        window << [Duration.ZERO, Duration.ofMinutes(-1)]
    }

    def "a missing grace window is refused"() {
        when:
        new MfaPolicy("REQUIRED", null, "")

        then:
        thrown(NullPointerException)
    }

    // ---------------------------------------------------------------- who is owed a grace window

    def "no cut-off means every account is owed a window"() {
        given:
        def policy = new MfaPolicy("REQUIRED", MfaPolicy.DEFAULT_GRACE, "")

        expect: "the behaviour of a deployment that has not configured this cannot change"
        policy.graceCutoff().isEmpty()
        policy.graceEligible(Instant.now())
        policy.graceEligible(null)
    }

    def "an account created before the cut-off is owed a window, one created after is not"() {
        given:
        def cutOff = Instant.parse("2026-11-20T00:00:00Z")
        def policy = new MfaPolicy("REQUIRED", MfaPolicy.DEFAULT_GRACE, cutOff.toString())

        expect:
        policy.graceCutoff().get() == cutOff
        policy.graceEligible(cutOff.minusSeconds(1))
        !policy.graceEligible(cutOff.plusSeconds(1))
        !policy.graceEligible(cutOff)
    }

    def "an unknown registration date is treated as eligible, never as new"() {
        given:
        def policy = new MfaPolicy("REQUIRED", MfaPolicy.DEFAULT_GRACE, "2026-11-20T00:00:00Z")

        expect: "a data gap must not turn into a mandatory-enrolment decision"
        policy.graceEligible(null)
    }

    def "a cut-off that is not an ISO instant fails at startup"() {
        when: "reading it as 'unset' would silently keep granting windows"
        new MfaPolicy("REQUIRED", MfaPolicy.DEFAULT_GRACE, "20.11.2026")

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("ISO-8601")
        e.message.contains("qbic.security.mfa.grace-cutoff")
    }

    def "a cut-off with surrounding whitespace is accepted"() {
        expect: "copy-pasted configuration should not fail, and should not change the meaning"
        new MfaPolicy("REQUIRED", MfaPolicy.DEFAULT_GRACE, " 2026-11-20T00:00:00Z ")
                .graceCutoff().get() == Instant.parse("2026-11-20T00:00:00Z")
    }
}
