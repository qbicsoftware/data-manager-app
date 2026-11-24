package life.qbic.identity.domain.model.mfa;

import static java.util.Objects.requireNonNull;

/**
 * <b>Whether an account may remove one of its second factors</b>
 *
 * <p>Removing a factor is not one question but three, and the answer depends on the deployment
 * posture. The rule that matters: <b>an account may never silently drop below one active factor</b>,
 * because that is how a user loses the second factor without ever deciding to.</p>
 *
 * <ul>
 *   <li><b>{@code OFF}</b> — nothing is asked at sign-in, so removing a factor cannot lock anyone out.
 *       Allowed. (The page is hidden under {@code OFF} anyway; the domain stays permissive so the
 *       kill switch does not leave orphaned factors nobody can clear.)</li>
 *   <li><b>{@code OPTIONAL}</b> — the account decides whether it has a second factor. Removing the
 *       <em>last</em> one is therefore allowed, but it is not an ordinary removal: it is "turn
 *       two-factor authentication off", and it must also retire the recovery codes, which are only
 *       meaningful while a factor exists. Reported as {@link Outcome#TURNS_OFF_TWO_FACTOR} so the
 *       caller has to run that flow rather than a plain removal.</li>
 *   <li><b>{@code REQUIRED}</b> — the account must keep at least one factor. Removing the last one is
 *       refused: {@link Outcome#LAST_FACTOR_PROTECTED}. There is no way to turn enforcement off from
 *       the account, by design.</li>
 * </ul>
 *
 * <p>This lives in the domain, not the UI, because it is the difference between "the button was
 * greyed out" and "the account cannot be left without a factor". A rule enforced only in a view is a
 * rule a second caller can ignore; {@code SecondFactorService} therefore consults this before every
 * removal.</p>
 *
 * @since 1.0.0
 */
public final class SecondFactorRemoval {

  private SecondFactorRemoval() {
  }

  /** What a removal request resolves to. */
  public enum Outcome {
    /** Remove this factor; at least one other active factor remains (or none is required). */
    ALLOWED,
    /**
     * This is the last active factor under {@code OPTIONAL}: allowed, but only as the explicit
     * "turn off two-factor authentication" action, which also deletes the account's recovery codes.
     */
    TURNS_OFF_TWO_FACTOR,
    /** This is the last active factor under {@code REQUIRED}: refused. */
    LAST_FACTOR_PROTECTED,
    /** The factor belongs to another account, or does not exist — reported identically on purpose. */
    NOT_REMOVABLE
  }

  /**
   * Decide a removal request.
   *
   * @param mode             the deployment posture
   * @param factorOwnedByUser whether the factor exists and belongs to the requesting account
   * @param activeFactorCount how many active factors the account has, including the target
   * @return the outcome the caller must act on
   */
  public static Outcome decide(MfaPolicyMode mode, boolean factorOwnedByUser, int activeFactorCount) {
    requireNonNull(mode, "mode must not be null");
    if (!factorOwnedByUser || activeFactorCount <= 0) {
      return Outcome.NOT_REMOVABLE;
    }
    if (mode == MfaPolicyMode.OFF) {
      return Outcome.ALLOWED;
    }
    if (activeFactorCount > 1) {
      return Outcome.ALLOWED;
    }
    return mode == MfaPolicyMode.OPTIONAL
        ? Outcome.TURNS_OFF_TWO_FACTOR
        : Outcome.LAST_FACTOR_PROTECTED;
  }

  /**
   * @param mode the deployment posture
   * @return {@code true} if an account may end up with no second factor at all
   */
  public static boolean twoFactorCanBeTurnedOff(MfaPolicyMode mode) {
    requireNonNull(mode, "mode must not be null");
    // OFF needs no switching off (nothing is on), OPTIONAL lets the account decide, REQUIRED does not.
    return mode != MfaPolicyMode.REQUIRED;
  }

  /**
   * @param mode the deployment posture
   * @return {@code true} if a user may start two-factor authentication themselves
   */
  public static boolean twoFactorCanBeEnabled(MfaPolicyMode mode) {
    requireNonNull(mode, "mode must not be null");
    // Under OFF nothing is asked at sign-in, so offering enrolment would be a dead end: the user does
    // the work, the login never asks for the code, and the settings page has to say so.
    return mode != MfaPolicyMode.OFF;
  }
}
