package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
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
   * The heading of the notice shown right after a recovery code was redeemed (MFA-41b).
   *
   * <p>This is an <em>event</em> notice, not a state summary — the state summary is
   * {@link #recoverySummary}. The distinction is what the QA run found missing: an account that spent
   * its last code is in a materially different situation from one that has five left, and only the
   * event tells it so at the moment it becomes true.</p>
   *
   * @param unusedCodes how many codes are left; {@code 0} means the set is spent
   * @return the heading
   */
  static String recoveryCodesLeftTitle(long unusedCodes) {
    return unusedCodes <= 0 ? "No recovery codes left" : "Recovery code used";
  }

  /**
   * The body of the notice shown right after a recovery code was redeemed.
   *
   * <p>For an empty set it names the loss and the one action that still exists: enrolling a new
   * authenticator issues a fresh set. It says nothing about e-mail, because mailbox control must never
   * read as the way past a factor (ADR 0011 B1) — and a user in exactly this state is the one most
   * likely to go looking for that shortcut.</p>
   *
   * @param unusedCodes how many codes are left; negative is treated as none
   * @return the body text
   */
  static String recoveryCodesLeftBody(long unusedCodes) {
    if (unusedCodes <= 0) {
      return "You signed in with your last recovery code, so none are left. Set up a new "
          + "authenticator app to get a fresh set — without one, losing access to your app means you "
          + "cannot sign in by yourself.";
    }
    return "You signed in with a recovery code. " + unusedCodes
        + (unusedCodes == 1 ? " code is" : " codes are")
        + " still unused. Set up a new authenticator app to get a fresh set.";
  }

  /**
   * @param unusedCodes unused codes on the account
   * @param hasFactor   whether the account has an active factor
   * @return the label of the recovery-code action
   */
  static String recoveryActionLabel(long unusedCodes, boolean hasFactor) {
    return hasFactor && unusedCodes > 0 ? "Generate a new set" : "Generate recovery codes";
  }

  /** What a factor is called when the user does not choose a name. */
  static final String DEFAULT_FACTOR_NAME = "Authenticator app";

  // ── Enrolment page copy ────────────────────────────────────────────────────
  //
  // Here rather than inline in the page for the same reason everything else on this subject is: the
  // enrolment screen exists twice (settings and mandatory-at-login), and two copies of the same
  // sentence is how they drift into saying different things.

  /** Title of the enrolment screen. Shared by both entry points. */
  static final String ENROL_TITLE = "Set up an authenticator app";

  /** The instruction above the QR code. */
  static final String ENROL_INTRO =
      "Scan the QR code with your authenticator app, then type the six-digit code it shows.";

  /** Shown when a phone camera cannot be used. */
  static final String ENROL_SETUP_KEY_LABEL = "Setup key (if you can't scan)";

  /** The primary action: verify the code, which is what creates the factor. */
  static final String ENROL_CONFIRM_LABEL = "Verify and enable";

  /** Labels the optional naming field; the word "optional" is load-bearing. */
  static final String ENROL_NAME_LABEL = "Name this app (optional)";

  /** Explains what the name is for, in one line. */
  static final String ENROL_NAME_HELPER = "Only used to tell your apps apart in settings.";

  /** Label of the code field. */
  static final String ENROL_CODE_LABEL = "Code from the app";

  // ── Recovery codes at enrolment ────────────────────────────────────────────

  /**
   * <b>Whether confirming an enrolment should also issue a recovery-code sheet.</b>
   *
   * <p>Only when the account has none left. A sheet is not a souvenir of adding an app: issuing one is
   * destructive, because {@code RecoveryCodeService.generate} retires every existing code in the same
   * transaction. A user adding a second authenticator <em>for redundancy</em> — the intended reason to
   * add one — would otherwise be handed a fresh sheet that silently kills the one they printed. Dismiss
   * it believing the old codes still work, and they hold ten dead codes and no way back in.</p>
   *
   * <p>So the rule is: first enrolment, or every code spent, or the user explicitly asks for a new set
   * on the settings page. Nothing else.</p>
   *
   * @param unusedCodes how many codes are still usable on the account
   * @return {@code true} if confirming this enrolment must show a new sheet
   */
  static boolean recoverySheetNeededOnEnrolment(long unusedCodes) {
    return unusedCodes <= 0;
  }

  /**
   * What the user is told after enrolling when no new sheet was issued.
   *
   * <p>The absence of a sheet is itself a message, and it must not be read as "you have no codes". The
   * user has just replaced or added an app; the sentence has to confirm that the codes they saved
   * earlier are untouched, and name the action that <em>does</em> replace them, so a deliberate
   * replacement stays one click away.</p>
   *
   * @param hasUnusedCodes whether the account actually has codes left
   * @return the notice under the enrolment confirmation
   */
  static String enrolmentDoneWithoutNewCodesBody(boolean hasUnusedCodes) {
    if (!hasUnusedCodes) {
      // Should not be reachable: no codes means a sheet was due. Say something true rather than
      // reproduce the sentence above, which would claim codes the account does not have.
      return "You have no unused recovery codes. Set them up from the two-factor settings page.";
    }
    return "Your existing recovery codes still work — adding an app does not change them. "
        + "If you ever want a fresh set, generate new recovery codes from the two-factor settings "
        + "page.";
  }

  // ── Deadlines ──────────────────────────────────────────────────────────────

  /**
   * How a grace deadline is shown: full date <b>and time of day</b>, in the user's zone.
   *
   * <p>The time is the point. A deadline is an instant, but "28 November 2026" renders half of it and
   * invites the reader to supply the rest — which they do generously, planning to enrol "that
   * evening". The window often ends mid-afternoon, and the user is locked out having believed they
   * had hours left. Reported from testing under {@code REQUIRED}.</p>
   *
   * <p>Local time, not UTC: the user is planning their day in the zone they are sitting in. Factor
   * timestamps elsewhere on the settings page keep their own date formatter — those are history, not
   * deadlines, and the distinction is deliberate.</p>
   */
  private static final DateTimeFormatter DEADLINE_FORMAT =
      DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ROOT)
          .withZone(ZoneId.systemDefault());

  /**
   * Formats a deadline for display, including the time of day.
   *
   * @param deadline the instant the grace window ends
   * @return e.g. {@code "28 November 2026, 15:42"}
   */
  static String formattedDeadline(Instant deadline) {
    requireNonNull(deadline, "deadline must not be null");
    return DEADLINE_FORMAT.format(deadline);
  }

  /**
   * Formats an instant as a date only.
   *
   * <p>For history — when a factor was added, when it was last used — where the time of day is noise
   * rather than information. Deliberately separate from {@link #formattedDeadline}, so a deadline
   * cannot be rendered with the date formatter by accident.</p>
   *
   * @param instant the instant to format
   * @return e.g. {@code "28 Nov 2026"}
   */
  static String formattedDate(Instant instant) {
    requireNonNull(instant, "instant must not be null");
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(instant);
  }

  /**
   * The label a factor gets, given what the user typed in the optional name field.
   *
   * <p>An empty field means the default, not "no name": the domain refuses a blank label, and failing the
   * submission <em>after</em> the code was accepted would be a confusing way to learn that a label is
   * required. The field is prefilled with the default, so this only matters when somebody clears it on
   * purpose.</p>
   *
   * @param typed what is in the field, possibly null, possibly only whitespace
   * @return the label to store
   */
  static String factorName(String typed) {
    String chosen = typed == null ? "" : collapseBlankToEmpty(typed);
    return chosen.isEmpty() ? DEFAULT_FACTOR_NAME : chosen.strip();
  }

  /**
   * Treats a value made only of <em>invisible</em> characters as empty.
   *
   * <p>{@link String#strip()} removes characters {@code Character.isWhitespace} accepts, and a
   * non-breaking space ({@code U+00A0}) is not one of them — it is a format character. A name pasted out
   * of a word processor is therefore frequently "not blank" while being completely invisible, which would
   * store a factor whose label renders as nothing at all. In a list whose whole purpose is telling your
   * apps apart, that is worse than an empty field.</p>
   *
   * @param value what the user typed
   * @return the value with invisible-only content reduced to the empty string
   */
  private static String collapseBlankToEmpty(String value) {
    // Character.isWhitespace covers tabs and spaces; the explicit cases cover the format characters that
    // pasting brings in and that the domain would happily accept.
    boolean invisible = value.codePoints().allMatch(codePoint ->
        Character.isWhitespace(codePoint)
            || Character.isSpaceChar(codePoint)
            || codePoint == 0x200B   // zero-width space
            || codePoint == 0xFEFF); // byte-order mark
    return invisible ? "" : value;
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
