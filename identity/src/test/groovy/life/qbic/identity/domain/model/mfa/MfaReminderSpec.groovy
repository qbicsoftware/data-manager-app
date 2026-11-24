package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>When a grace window is owed a reminder.</b>
 *
 * <p>Two mails stand between a deadline and a lockout, and the failure modes are both quiet: a reminder
 * that is decided by comparing clocks re-sends on every pass, and one that only looks at a time window
 * skips the account entirely when the job was down. The stamps on the window are the only state that
 * makes the decision idempotent, so they are what these tests exercise.</p>
 */
class MfaReminderSpec extends Specification {

    static final Instant START = Instant.parse("2026-11-20T09:00:00Z")
    static final Duration WINDOW = Duration.ofHours(48)
    static final Duration SOON = Duration.ofHours(24)
    static final Duration FINAL = Duration.ofHours(2)

    // deadline is START + 48h = 2026-11-22T09:00:00Z
    static final Instant DEADLINE = START.plus(WINDOW)

    private static MfaGrace grace() {
        MfaGrace.start("u-1", START, WINDOW)
    }

    private static MfaReminder.Stage dueAt(Instant now, MfaGrace window = grace()) {
        MfaReminder.due(window, now, SOON, FINAL)
    }

    def "nothing is due while there is more than a day left"() {
        expect:
        dueAt(DEADLINE.minus(Duration.ofHours(25))) == MfaReminder.Stage.NONE
    }

    def "the first reminder becomes due a day before the deadline"() {
        expect:
        dueAt(DEADLINE.minus(SOON)) == MfaReminder.Stage.SOON
        dueAt(DEADLINE.minus(Duration.ofHours(10))) == MfaReminder.Stage.SOON
    }

    def "the final reminder takes over two hours before the deadline"() {
        expect: "a job that was down for a day must not send both mails minutes apart"
        dueAt(DEADLINE.minus(FINAL)) == MfaReminder.Stage.FINAL
        dueAt(DEADLINE.minus(Duration.ofMinutes(1))) == MfaReminder.Stage.FINAL
    }

    def "a sent reminder is not sent again, however often the job runs"() {
        given:
        def window = grace()

        when: "the first reminder was recorded as sent"
        markDueSent(window)

        then: "still before the final window, and the first one is spent"
        (0..10).every { dueAt(DEADLINE.minus(Duration.ofHours(10)), window) == MfaReminder.Stage.NONE }
    }

    def "one mail, not two, when the job first runs inside the final window"() {
        given: "the job was down for a day, so the first reminder's window has already passed"
        def window = grace()
        def now = DEADLINE.minus(Duration.ofHours(1))

        expect: "the final window decides, and the stale 'one day left' mail is never sent"
        dueAt(now, window) == MfaReminder.Stage.FINAL

        when:
        markFinalSent(window)

        then: "and afterwards there is nothing left to say"
        dueAt(now, window) == MfaReminder.Stage.NONE
        dueAt(now.plus(Duration.ofMinutes(30)), window) == MfaReminder.Stage.NONE
    }

    def "both reminders sent means silence"() {
        given:
        def window = grace()
        markDueSent(window)
        markFinalSent(window)

        expect:
        dueAt(DEADLINE.minus(Duration.ofHours(1)), window) == MfaReminder.Stage.NONE
    }

    @Unroll
    def "an expired window is not reminded, at #offset after the deadline"() {
        expect: "the message at that point is the recovery path, not a date that has passed"
        dueAt(DEADLINE.plus(offset)) == MfaReminder.Stage.NONE

        where:
        offset << [Duration.ZERO, Duration.ofMinutes(1), Duration.ofDays(3)]
    }

    def "a zero or negative offset is refused at the boundary, not silent"() {
        when:
        MfaReminder.due(grace(), DEADLINE, Duration.ZERO, FINAL)

        then:
        thrown(IllegalArgumentException)
    }

    def "the final reminder must come after the first"() {
        when: "otherwise 'one day left' and 'a few hours left' would be the same mail"
        MfaReminder.due(grace(), DEADLINE, Duration.ofHours(2), Duration.ofHours(24))

        then:
        thrown(IllegalArgumentException)
    }

    def "null arguments are refused"() {
        when:
        MfaReminder.due(null, DEADLINE, SOON, FINAL)

        then:
        thrown(NullPointerException)

        when:
        MfaReminder.due(grace(), null, SOON, FINAL)

        then:
        thrown(NullPointerException)
    }

    // The entity's stamps are private and set by the repository in production; here they are set
    // reflectively so the spec exercises the real object rather than a stub of it.
    private static void markDueSent(MfaGrace window) {
        set(window, "reminderDueSentAt", START.plus(Duration.ofHours(25)))
    }

    private static void markFinalSent(MfaGrace window) {
        set(window, "reminderFinalSentAt", START.plus(Duration.ofHours(46)))
    }

    private static void set(MfaGrace window, String field, Instant value) {
        def f = MfaGrace.getDeclaredField(field)
        f.setAccessible(true)
        f.set(window, value)
    }
}
