package life.qbic.datamanager.views.account;

import life.qbic.identity.domain.model.mfa.MfaPolicyMode;

/**
 * <b>Every sentence the two-factor settings page can show, decided in one place</b>
 *
 * <p>This page states two different kinds of fact and they must never be confused:</p>
 * <ol>
 *   <li><b>what this account has</b> — a factor, and how many unused recovery codes;</li>
 *   <li><b>what this installation asks for at sign-in</b> — the {@link MfaPolicyMode}.</li>
 * </ol>
 *
 * <p>The first version of the page decided those sentences in three independent setters, and each
 * one knew only its own input. The result was two statements that were not true of the page they
 * appeared on, both of which a UI review found from the code alone:</p>
 * <ul>
 *   <li>the enrolled summary claimed the app <em>"is required the next time you sign in"</em> while
 *       the policy note said two-factor authentication is <em>not required at sign-in</em> (mode
 *       {@code OFF});</li>
 *   <li>mode {@code OPTIONAL} claimed <em>"you will be asked for a code, because this account has set
 *       up a factor"</em> on an account that had set up nothing — where, correctly, nothing is
 *       asked.</li>
 * </ul>
 *
 * <p>So the sentences are pure functions of all three inputs (mode, factor presence, unused-code
 * count), and {@code TwoFactorSettingsCopySpec} pins the truth table: no sentence may claim an
 * enforcement the mode does not perform, and no sentence may advertise a reset by e-mail — accounts
 * are self-service, so mailbox control must never become the way past the factor.</p>
 *
 * @since 1.0.0
 */
public final class TwoFactorSettingsCopy {

  private TwoFactorSettingsCopy() {
  }

  /**
   * States what the account has. Deliberately says nothing about whether the app will be asked for:
   * that belongs to {@link #policyNote(MfaPolicyMode, boolean)}, the only sentence that knows the
   * mode.
   *
   * @param factorCount active factors on the account
   * @return the status sentence
   */
  static String factorSummary(int factorCount) {
    if (factorCount <= 0) {
      return "No authenticator app is set up for this account yet.";
    }
    if (factorCount == 1) {
      return "One authenticator app is set up.";
    }
    return "%d authenticator apps are set up. Any of them can be used to sign in."
        .formatted(factorCount);
  }

  /**
   * States what this installation asks for at sign-in, given what the account has.
   *
   * <p>The factor argument is the whole point: {@code OPTIONAL} with no factor asks for nothing, so
   * the sentence must not promise a challenge. Under {@code OFF} the answer is always "not asked",
   * even for an enrolled account — {@code MfaGate} returns {@code NotRequired} regardless of the
   * factor, and saying otherwise would be a promise the deployment does not keep.</p>
   *
   * @param mode      the posture the running application was started with
   * @param hasFactor whether the account has an active factor
   * @return the posture sentence
   */
  static String policyNote(MfaPolicyMode mode, boolean hasFactor) {
    return switch (mode) {
      case OFF -> "Two-factor authentication is turned off on this installation: you will not be "
          + "asked for a code at sign-in, even after you set up an app.";
      case OPTIONAL -> hasFactor
          ? "You will be asked for a code from your app at sign-in."
          : "Two-factor authentication is optional on this installation. Without an app you sign in "
              + "with your password alone.";
      case REQUIRED -> hasFactor
          ? "Two-factor authentication is required at sign-in for every account. If you remove your "
              + "last app you will not be able to sign in until you set up a new one."
          : "Two-factor authentication is required at sign-in for every account. You can still sign "
              + "in until the grace period for your account ends.";
    };
  }

  /**
   * The sentence for an unenrolled account under {@code REQUIRED}, naming the exact deadline.
   *
   * <p>"Until the grace period ends" is true but useless: the only thing that user needs to know is
   * <em>when</em>. There is deliberately no second variant for "the window has already ended" — an
   * account whose window is over cannot sign in, so it never reaches this page, and copy for an
   * unreachable state is copy nobody can check.</p>
   *
   * @param formattedEnd the deadline, already formatted for display
   * @return the posture sentence
   */
  static String requiredDeadlineNote(String formattedEnd) {
    return "Two-factor authentication is required at sign-in. You can still sign in with your "
        + "password until " + formattedEnd + " — after that, only an app gets you in.";
  }

  /**
   * States the recovery-code situation.
   *
   * <p>Without a factor there is no challenge, so a generated set could never be redeemed. The
   * page must therefore say that the codes come after the app, rather than hand out a one-time
   * sheet that cannot be used.</p>
   *
   * @param unusedCodes unused codes on the account
   * @param hasFactor   whether the account has an active factor
   * @return the recovery-code sentence
   */
  static String recoverySummary(long unusedCodes, boolean hasFactor) {
    if (!hasFactor) {
      return "Available once an authenticator app is set up. The codes are shown once, right after "
          + "your first app is confirmed.";
    }
    if (unusedCodes <= 0) {
      return "No unused recovery codes. Losing your phone without them means an administrator has "
          + "to re-enrol you.";
    }
    if (unusedCodes == 1) {
      return "1 unused recovery code left. Generating a new set stops it working.";
    }
    return "%d unused recovery codes left. Generating a new set stops the current codes working."
        .formatted(unusedCodes);
  }

  /**
   * @param unusedCodes unused codes on the account
   * @param hasFactor   whether the account has an active factor
   * @return the label of the recovery-code action
   */
  static String recoveryActionLabel(long unusedCodes, boolean hasFactor) {
    return hasFactor && unusedCodes > 0 ? "Generate a new set" : "Generate recovery codes";
  }

  /**
   * @param hasFactor whether the account already has an active factor
   * @return the label of the enrolment action
   */
  static String enrolActionLabel(boolean hasFactor) {
    return hasFactor ? "Add another authenticator app" : "Set up authenticator app";
  }

  /**
   * The heading of the not-set-up card.
   *
   * <p>Under {@code REQUIRED} it is a deadline, not an invitation: the same card has to convey that
   * doing nothing has a cost. Under {@code OPTIONAL} it is an offer.</p>
   *
   * @param mode the posture
   * @return the empty-state heading
   */
  static String emptyStateTitle(MfaPolicyMode mode) {
    return mode == MfaPolicyMode.REQUIRED
        ? "Set up an authenticator app to stay signed in"
        : "Nothing set up yet.";
  }

  /**
   * @param mode the posture
   * @return the hint under the empty-state heading
   */
  static String emptyStateHint(MfaPolicyMode mode) {
    return mode == MfaPolicyMode.REQUIRED
        ? "You will need a phone with an authenticator app. Setting one up takes about a minute, and "
            + "you will be asked for a code from it from then on."
        : "You will need a phone with an authenticator app. Setting one up takes about a minute.";
  }
}
