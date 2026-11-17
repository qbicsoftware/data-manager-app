package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

import static life.qbic.identity.domain.model.mfa.MfaGateDecision.Challenge
import static life.qbic.identity.domain.model.mfa.MfaGateDecision.GraceActive
import static life.qbic.identity.domain.model.mfa.MfaGateDecision.GraceExpired
import static life.qbic.identity.domain.model.mfa.MfaGateDecision.GraceGranted
import static life.qbic.identity.domain.model.mfa.MfaGateDecision.NotRequired

/**
 * <b>The decision that stands between a password and a session (AUTH-R-01).</b>
 *
 * <p>{@link MfaGate#decide} is the single place that answers "may this account proceed, must it
 * present a factor, or must it be denied". The assertions below are therefore the security rules
 * themselves, not a convenience layer — the login filter added in M4 must not re-derive any of
 * them.</p>
 *
 * <p>Two groups of tests matter more than the rest:</p>
 * <ul>
 *   <li><b>The lockout group.</b> {@code REQUIRED} + unenrolled + <em>no stored window</em> must
 *       grant a window. Reading the absence as "grace is over" denies the whole population at the
 *       instant the policy changes (ADR 0011 item 3, invariant 1).</li>
 *   <li><b>The kill-switch group.</b> {@code OFF} must not challenge even enrolled accounts,
 *       otherwise the mode cannot take a broken factor store out of the login path.</li>
 * </ul>
 */
class MfaGateSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-17T09:00:00Z")
    private static final Duration TWO_DAYS = Duration.ofHours(48)

    private static MfaGateDecision decide(MfaPolicyMode mode, boolean enrolled,
                                          MfaGrace grace = null, Instant now = NOW) {
        MfaGate.decide(mode, enrolled, Optional.ofNullable(grace), now, TWO_DAYS)
    }

    private static MfaGrace graceActiveUntil(Instant deadline) {
        MfaGrace.start("u-1", deadline.minus(TWO_DAYS), TWO_DAYS)
    }

    // ---------------------------------------------------------------- kill switch (OFF)

    @Unroll
    def "OFF never requires a factor, enrolled=#enrolled"() {
        expect:
        decide(MfaPolicyMode.OFF, enrolled) instanceof NotRequired

        where:
        enrolled << [true, false]
    }

    def "OFF does not even look at the grace window, and does not validate a missing one"() {
        expect: "a deployment in OFF mode needs no grace configuration at all"
        MfaGate.decide(MfaPolicyMode.OFF, false, Optional.empty(), NOW, null) instanceof NotRequired
    }

    // ---------------------------------------------------------------- enrolled accounts

    @Unroll
    def "an enrolled account is challenged whenever the policy is not OFF (#mode)"() {
        expect: "enrolling must never be a downgrade for the user"
        decide(mode, true) instanceof Challenge

        where:
        mode << [MfaPolicyMode.OPTIONAL, MfaPolicyMode.REQUIRED]
    }

    def "an enrolled account is challenged even while a grace window is still running"() {
        given: "a user who enrolled after their window was granted"
        def grace = graceActiveUntil(NOW.plus(Duration.ofHours(1)))

        expect: "enrolment ends the grace discussion; the factor is presented from now on"
        decide(MfaPolicyMode.REQUIRED, true, grace) instanceof Challenge
    }

    // ---------------------------------------------------------------- OPTIONAL, unenrolled

    def "OPTIONAL lets an unenrolled account through without a factor and without a window"() {
        expect:
        decide(MfaPolicyMode.OPTIONAL, false) instanceof NotRequired
    }

    // ---------------------------------------------------------------- REQUIRED, unenrolled: the grace window

    def "REQUIRED with no stored window GRANTS one derived from now, not from the row"() {
        when:
        def decision = decide(MfaPolicyMode.REQUIRED, false)

        then: "the caller is told exactly what to persist"
        decision instanceof GraceGranted
        (decision as GraceGranted).graceUntil() == NOW.plus(TWO_DAYS)
        decision.loginMayProceedWithoutFactor()
        !decision.secondFactorRequired()
    }

    def "the granted window is measured from the account's first login, not from deployment"() {
        given: "a user who first logs in three weeks after the policy flip"
        def lateLogin = NOW.plus(Duration.ofDays(21))

        when:
        def decision = decide(MfaPolicyMode.REQUIRED, false, null, lateLogin)

        then: "the full window is still ahead of them; a deployment-anchored deadline would be long gone"
        (decision as GraceGranted).graceUntil() == lateLogin.plus(TWO_DAYS)
    }

    def "a running window is honoured and reported with its stored deadline"() {
        given:
        def deadline = NOW.plus(Duration.ofHours(30))
        def grace = graceActiveUntil(deadline)

        when:
        def decision = decide(MfaPolicyMode.REQUIRED, false, grace)

        then:
        decision instanceof GraceActive
        (decision as GraceActive).graceUntil() == deadline
        decision.loginMayProceedWithoutFactor()
    }

    def "the deadline itself is already too late: grace expires at the boundary, not after it"() {
        given: "the same boundary rule as VerificationToken.isExpiredAt"
        def deadline = NOW
        def grace = graceActiveUntil(deadline)

        expect:
        decide(MfaPolicyMode.REQUIRED, false, grace) instanceof GraceExpired
    }

    def "an expired window denies and carries the deadline that passed"() {
        given:
        def deadline = NOW.minus(Duration.ofMinutes(1))
        def grace = graceActiveUntil(deadline)

        when:
        def decision = decide(MfaPolicyMode.REQUIRED, false, grace)

        then: "denied, but the reason is reportable without a second lookup"
        decision instanceof GraceExpired
        (decision as GraceExpired).graceUntil() == deadline
        decision.secondFactorRequired()
        !decision.loginMayProceedWithoutFactor()
    }

    // ---------------------------------------------------------------- invariants

    def "deciding twice with an absent window yields the same deadline — grace is not a random draw"() {
        when:
        def first = decide(MfaPolicyMode.REQUIRED, false)
        def second = decide(MfaPolicyMode.REQUIRED, false)

        then: "the same instant produces the same deadline, so a lost persist cannot silently move it"
        (first as GraceGranted).graceUntil() == (second as GraceGranted).graceUntil()
    }

    def "the gate is pure: it never mutates the stored window"() {
        given:
        def grace = graceActiveUntil(NOW.plus(Duration.ofHours(1)))
        def deadlineBefore = grace.graceUntil()

        when:
        decide(MfaPolicyMode.REQUIRED, false, grace)

        then:
        grace.graceUntil() == deadlineBefore
        grace.isActiveAt(NOW)
    }

    @Unroll
    def "exactly one of the two questions is true for #decision.class.simpleName"() {
        expect: "a decision can never both allow and require the factor"
        decision.loginMayProceedWithoutFactor() != decision.secondFactorRequired()

        where:
        decision << [new NotRequired(), new Challenge(),
                     new GraceGranted(NOW), new GraceActive(NOW), new GraceExpired(NOW)]
    }

    // ---------------------------------------------------------------- misconfiguration

    def "a non-positive grace window is refused when it would be granted"() {
        when: "REQUIRED would have to grant a window"
        MfaGate.decide(MfaPolicyMode.REQUIRED, false, Optional.empty(), NOW, window)

        then: "failing loudly beats granting a zero-second window, which is a lockout with extra steps"
        thrown(IllegalArgumentException)

        where:
        window << [Duration.ZERO, Duration.ofSeconds(-1)]
    }

    def "a missing grace window is only validated where it is used"() {
        expect: "OFF and OPTIONAL deployments must not fail to start over configuration they never read"
        MfaGate.decide(mode, false, Optional.empty(), NOW, null) instanceof NotRequired

        where:
        mode << [MfaPolicyMode.OFF, MfaPolicyMode.OPTIONAL]
    }

    def "null arguments are rejected"() {
        when:
        MfaGate.decide(null, false, Optional.empty(), NOW, TWO_DAYS)

        then:
        thrown(NullPointerException)

        when:
        MfaGate.decide(MfaPolicyMode.OPTIONAL, false, null, NOW, TWO_DAYS)

        then:
        thrown(NullPointerException)
    }

    def "a missing grace window is a configuration error, not a zero-second window"() {
        when: "REQUIRED would have to grant a window and none is configured"
        MfaGate.decide(MfaPolicyMode.REQUIRED, false, Optional.empty(), NOW, null)

        then: "fail loudly at the decision, not by silently denying everyone"
        thrown(NullPointerException)
    }
}
