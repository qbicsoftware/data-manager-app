package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * <b>The second-factor gate</b>
 *
 * <p>The single decision point that answers, for one account that has just presented its password:
 * <em>may a session be established, must a factor be presented first, or must the login be denied?</em>
 * It is a pure function — no repository, no clock, no Spring — so the rules of AUTH-R-01 and the four
 * grace-window invariants of ADR 0011 are testable without a database, and the login filter that
 * calls it has no room to re-derive them.</p>
 *
 * <h3>Evaluation order, and why it is that order</h3>
 * <ol>
 *   <li><b>{@link MfaPolicyMode#OFF} wins over everything.</b> It is the kill switch; a mode that
 *       still challenged enrolled users would keep the broken code path live while claiming to turn
 *       it off.</li>
 *   <li><b>An enrolled account is always challenged</b> (except under {@code OFF}), including under
 *       {@link MfaPolicyMode#OPTIONAL}. The opposite — challenging only when policy is
 *       {@code REQUIRED} — would make enrolment a downgrade for the user: they invest in a second
 *       factor and get nothing until the rollout reaches their cohort.</li>
 *   <li><b>Under {@code OPTIONAL}, an unenrolled account proceeds.</b> That is what optional means;
 *       the user is offered enrolment without being forced.</li>
 *   <li><b>Under {@code REQUIRED}, an unenrolled account gets the grace window</b> — granted if it has
 *       none, honoured if it is running, denied once it has passed.</li>
 * </ol>
 *
 * <h3>The lockout bug this type exists to prevent</h3>
 * <p>An unenrolled account under {@code REQUIRED} with <em>no stored window</em> must be
 * <b>granted</b> one ({@link MfaGateDecision.GraceGranted}), never denied. The naive implementation
 * treats a missing deadline as "grace is over", which denies every unenrolled account at the instant
 * the configuration changes — a total lockout of the entire population, caused by the very change
 * meant to protect it. Because the decision returns <em>what to persist</em>, a caller cannot
 * accidentally implement the naive version: ignoring a {@code GraceGranted} leaves the account
 * without a deadline, which grants again on the next login rather than denying.</p>
 *
 * <p>The gate never mutates anything and never consults "now" internally; both the stored window and
 * the current instant are parameters, so the decision is reproducible and the caller owns the
 * transaction that persists {@link MfaGateDecision.GraceGranted}.</p>
 *
 * @since 1.0.0
 */
public final class MfaGate {

  private MfaGate() {
  }

  /**
   * Decides what the login path must do for one account.
   *
   * @param mode             the deployment posture; must not be null
   * @param hasActiveFactor  whether the account has at least one factor that is not disabled. Must be
   *     derived from active factors only: a disabled factor is not enrolment (AUTH-R-02).
   * @param grace            the stored grace window of the account, if any. An absent value is not
   *     "no grace" — see the class documentation.
   * @param now              the instant of the decision; must not be null
   * @param graceWindow      the window to grant when there is none stored. Must be positive; only
   *     validated when it is actually used, because a deployment that never reaches
   *     {@code REQUIRED} does not need one and should not fail to start over it.
   * @return what the login path must do, including any deadline it must persist
   */
  public static MfaGateDecision decide(MfaPolicyMode mode, boolean hasActiveFactor,
      Optional<MfaGrace> grace, Instant now, Duration graceWindow) {
    requireNonNull(mode, "mode must not be null");
    requireNonNull(grace, "grace must not be null");
    requireNonNull(now, "now must not be null");

    if (mode == MfaPolicyMode.OFF) {
      return new MfaGateDecision.NotRequired();
    }
    if (hasActiveFactor) {
      return new MfaGateDecision.Challenge();
    }
    if (mode == MfaPolicyMode.OPTIONAL) {
      return new MfaGateDecision.NotRequired();
    }

    // REQUIRED, no active factor.
    if (grace.isPresent()) {
      MfaGrace window = grace.get();
      return window.isActiveAt(now)
          ? new MfaGateDecision.GraceActive(window.graceUntil())
          : new MfaGateDecision.GraceExpired(window.graceUntil());
    }
    requirePositive(graceWindow);
    return new MfaGateDecision.GraceGranted(now.plus(graceWindow));
  }

  private static void requirePositive(Duration graceWindow) {
    requireNonNull(graceWindow,
        "graceWindow must not be null when an account becomes subject to required mode");
    if (graceWindow.isZero() || graceWindow.isNegative()) {
      throw new IllegalArgumentException(
          "graceWindow must be positive, but was " + graceWindow
              + " — a zero window denies every unenrolled account at the moment the policy changes");
    }
  }
}
