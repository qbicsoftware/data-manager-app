package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;

/**
 * <b>Which reminder, if any, an account's grace window is owed right now</b>
 *
 * <p>The last few days of a grace window are the only time at which a reminder can change the
 * outcome: before them there is nothing to act on, after them the recovery path is already the only
 * way in. Two reminders, a day and a couple of hours before the deadline, move users out of the
 * population that would otherwise meet the deadline as a lockout.</p>
 *
 * <p><b>Pure, and idempotent by construction.</b> The decision reads the two "already sent" stamps on
 * the window, so running the job twice, restarting the service, or running it on a second instance
 * cannot send the same reminder twice. Time-window guessing ("send if the deadline is between 20 and 24
 * hours away") looks simpler and fails exactly where it matters: a job that was down for six hours skips
 * the reminder entirely, and one that runs every minute sends it thirty times.</p>
 *
 * @since 1.0.0
 */
public final class MfaReminder {

  private MfaReminder() {
  }

  /**
   * What to send, if anything.
   *
   * @since 1.0.0
   */
  public enum Stage {
    /** Nothing is owed: too early, already sent, or the window is over. */
    NONE,
    /** "About a day left". */
    SOON,
    /** "A few hours left" — the last reminder before the deadline. */
    FINAL
  }

  /**
   * Decides what the account is owed at this instant.
   *
   * @param grace        the window; must not be null
   * @param now          the instant of the decision; must not be null
   * @param soonBefore   how long before the deadline the first reminder is due
   * @param finalBefore  how long before the deadline the final reminder is due; must be shorter than
   *                     {@code soonBefore}
   * @return the reminder to send, or {@link Stage#NONE}
   */
  public static Stage due(MfaGrace grace, Instant now, Duration soonBefore, Duration finalBefore) {
    requireNonNull(grace, "grace must not be null");
    requireNonNull(now, "now must not be null");
    requireDuration(soonBefore, "soonBefore");
    requireDuration(finalBefore, "finalBefore");
    if (finalBefore.compareTo(soonBefore) >= 0) {
      throw new IllegalArgumentException(
          "the final reminder must come after the first one (soonBefore=" + soonBefore
              + ", finalBefore=" + finalBefore + ")");
    }

    if (!grace.isActiveAt(now)) {
      // The window is over: the deadline has passed, and a reminder for a past date is noise at the
      // very moment the user needs the recovery path instead.
      return Stage.NONE;
    }
    // The final window is evaluated first, and it *decides* rather than falling through: once the
    // deadline is within the final offset, an unsent first reminder is already stale — a job that was
    // down for a day would otherwise send "one day left" minutes after "a few hours left".
    if (!now.isBefore(grace.graceUntil().minus(finalBefore))) {
      return grace.reminderFinalSentAt() == null ? Stage.FINAL : Stage.NONE;
    }
    if (!now.isBefore(grace.graceUntil().minus(soonBefore))) {
      return grace.reminderDueSentAt() == null ? Stage.SOON : Stage.NONE;
    }
    return Stage.NONE;
  }

  /**
   * @return {@code true} if the account is owed <em>some</em> reminder at this instant
   */
  public static boolean isDue(MfaGrace grace, Instant now, Duration soonBefore,
      Duration finalBefore) {
    return due(grace, now, soonBefore, finalBefore) != Stage.NONE;
  }

  private static void requireDuration(Duration value, String name) {
    requireNonNull(value, name + " must not be null");
    if (value.isZero() || value.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive, but was " + value);
    }
  }
}
