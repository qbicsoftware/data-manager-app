package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>The window that makes a policy flip survivable (ADR 0011 item 3).</b>
 *
 * <p>Two properties are pinned here because they are the difference between a usable deadline and a
 * lockout: the deadline is derived from the moment the account becomes subject to the policy, and
 * reaching the deadline counts as expired (the same boundary rule as
 * {@code VerificationToken.isExpiredAt}).</p>
 */
class MfaGraceSpec extends Specification {

    private static final Instant FIRST_LOGIN = Instant.parse("2026-11-17T10:00:00Z")
    private static final Duration TWO_DAYS = Duration.ofHours(48)

    def "a window starts at the first login and lasts the configured time"() {
        when:
        def grace = MfaGrace.start("u-1", FIRST_LOGIN, TWO_DAYS)

        then:
        grace.userId() == "u-1"
        grace.startedAt() == FIRST_LOGIN
        grace.graceUntil() == FIRST_LOGIN.plus(TWO_DAYS)
        grace.isActiveAt(FIRST_LOGIN)
    }

    def "the window is active strictly before the deadline"() {
        given:
        def grace = MfaGrace.start("u-1", FIRST_LOGIN, TWO_DAYS)

        expect:
        grace.isActiveAt(grace.graceUntil().minusSeconds(1))
        !grace.isExpiredAt(grace.graceUntil().minusSeconds(1))

        and: "reaching the deadline counts as expired, the same boundary rule as VerificationToken.isExpiredAt"
        !grace.isActiveAt(grace.graceUntil())
        grace.isExpiredAt(grace.graceUntil())
    }

    def "the window is not extended by continued use — it is a deadline, not a timer"() {
        given:
        def grace = MfaGrace.start("u-1", FIRST_LOGIN, TWO_DAYS)
        def deadline = grace.graceUntil()

        when: "the user logs in every day"
        (1..5).each { day -> grace.isActiveAt(FIRST_LOGIN.plus(Duration.ofDays(day))) }

        then: "the same deadline decides, and it is not renewable by logging in"
        grace.graceUntil() == deadline
        grace.isExpiredAt(FIRST_LOGIN.plus(Duration.ofDays(3)))
    }

    @Unroll
    def "a non-positive window is refused (#window)"() {
        when:
        MfaGrace.start("u-1", FIRST_LOGIN, window)

        then: "a zero window is a lockout with extra steps"
        thrown(IllegalArgumentException)

        where:
        window << [Duration.ZERO, Duration.ofHours(-1)]
    }

    def "the user id and the start instant are required"() {
        when:
        MfaGrace.start(" ", FIRST_LOGIN, TWO_DAYS)

        then:
        thrown(IllegalArgumentException)

        when:
        MfaGrace.start("u-1", null, TWO_DAYS)

        then:
        thrown(NullPointerException)

        when:
        MfaGrace.start("u-1", FIRST_LOGIN, null)

        then: "a missing window is a configuration error, not a zero window"
        thrown(NullPointerException)

    }
}
