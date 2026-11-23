package life.qbic.datamanager.views.account;

import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.model.mfa.OtpauthUri;

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
      return "No unused recovery codes. Losing access to your app without them means an administrator "
          + "has to re-enrol you.";
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
   * The sentence naming the entry the authenticator app should create.
   *
   * <p>It exists because the entry's name lives only inside the QR code, so a user whose app shows
   * something else has no way to tell whether the application sent the wrong label or the app
   * ignored it. That question is not academic: two accounts on the same mail domain, enrolled in the
   * same app, look alike unless the name is stated. Naming it also tells the user which account they
   * are enrolling, which is the other thing that is easy to get wrong with more than one account.</p>
   *
   * @param accountEmail the account the factor belongs to
   * @return the hint shown above the QR code
   */
  static String enrolmentEntryHint(String accountEmail) {
    return "In your app this entry appears under “" + OtpauthUri.ISSUER + "”, with "
        + accountEmail + " underneath. If your app shows something else, it is not using the name "
        + "in the QR code — enter the setup key by hand and name the entry yourself.";
  }

  /**
   * @param mode the posture
   * @return the hint under the empty-state heading
   */
  static String emptyStateHint(MfaPolicyMode mode) {
    return mode == MfaPolicyMode.REQUIRED
        ? "You will need an authenticator app — on your phone or in your password manager. Setting "
            + "one up takes about a minute, and you will be asked for a code from it from then on."
        : "You will need an authenticator app — on your phone or in your password manager. Setting "
            + "one up takes about a minute.";
  }

  /**
   * The heading of the in-app prompt that reaches accounts which never open the settings page.
   *
   * <p>Under {@code OPTIONAL} it is an offer, so it must not sound like a warning. Under
   * {@code REQUIRED} it is a deadline, and the heading says what is at stake rather than what is
   * recommended.</p>
   *
   * @param mode the posture
   * @return the prompt heading
   */
  static String promptTitle(MfaPolicyMode mode) {
    return mode == MfaPolicyMode.REQUIRED
        ? "Set up an authenticator app to keep signing in"
        : "Add a second step to your sign-in";
  }

  /**
   * The body of the in-app prompt.
   *
   * <p>Under {@code REQUIRED} the deadline is named when the page knows it: "before the grace period
   * ends" is true and useless, and a countdown the user cannot see is a countdown they cannot act
   * on. Under {@code OPTIONAL} there is no deadline to name, and inventing one would be the promise
   * this milestone forbids.</p>
   *
   * @param mode              the posture
   * @param formattedDeadline the grace deadline, already formatted, or {@code null} when there is
   *                          none to name
   * @return the prompt body
   */
  static String promptBody(MfaPolicyMode mode, String formattedDeadline) {
    if (mode == MfaPolicyMode.REQUIRED) {
      return formattedDeadline == null
          ? "Two-factor authentication is required on this installation. Signing in needs a code "
              + "from an app you set up beforehand."
          : "Two-factor authentication becomes required for your account on " + formattedDeadline
              + ". After that, signing in needs a code from an app.";
    }
    return "A stolen password is not enough on its own. Set up an authenticator app — on your phone "
        + "or in your password manager — and a leaked password stays useless.";
  }

  /**
   * Whether the prompt may be dismissed for the rest of the session.
   *
   * <p>An offer may be declined; a deadline may not be hidden. Under {@code REQUIRED} the prompt
   * therefore has no "not now" — the account is counting down and the reminder is the only thing
   * standing between the user and a lockout.</p>
   *
   * @param mode the posture
   * @return {@code true} if a "not now" action is honest
   */
  static boolean promptDismissible(MfaPolicyMode mode) {
    return mode != MfaPolicyMode.REQUIRED;
  }

  /**
   * Whether the in-app prompt belongs on the screen at all.
   *
   * <p>Three conditions, and each one is a way the prompt could be wrong: under {@code OFF} nothing
   * is asked at sign-in, so the prompt would send the user to a page that reroutes away; an account
   * that already has a factor has nothing to do; and a dismissed prompt must stay dismissed for the
   * session, or it becomes the nagging banner people learn to ignore.</p>
   *
   * @param mode      the posture
   * @param hasFactor whether the account has an active factor
   * @param dismissed whether the user dismissed the prompt in this session
   * @return {@code true} if the prompt should be rendered
   */
  static boolean promptVisible(MfaPolicyMode mode, boolean hasFactor, boolean dismissed) {
    return mode != MfaPolicyMode.OFF && !hasFactor && !dismissed;
  }
}
