package life.qbic.identity.api;

/**
 * <b>Password rules shared across context boundaries</b>
 *
 * <p>The identity domain enforces the password rule; the UI must state it. Before this class those
 * two facts lived apart: {@code PasswordPolicy} enforced {@value #MIN_LENGTH} characters while
 * rendering "shorter than 8 characters", and the registration and reset forms each carried their own
 * hard-coded copy of the number and of the hint text. A user who typed nine characters therefore saw
 * a message describing a rule that was not being applied.
 *
 * <p>The constants and the message live here — in the API module — for the same reason
 * {@link UserNames} does: so that the domain (which enforces) and the views (which explain) share one
 * source instead of duplicating it. Changing the rule changes one number.
 *
 * @since 1.19.0
 */
public final class Passwords {

  /**
   * The minimum number of characters a password must contain.
   */
  public static final int MIN_LENGTH = 12;

  private Passwords() {
    // utility class
  }

  /**
   * The message shown when a password is too short.
   *
   * <p>Derived from {@link #MIN_LENGTH} rather than written out, so it cannot go stale again. Views
   * render this verbatim; they must not build their own wording.
   *
   * @return the rejection message, stating the actual minimum
   * @since 1.19.0
   */
  public static String tooShortMessage() {
    return "Password must contain at least %d characters".formatted(MIN_LENGTH);
  }

  /**
   * The helper text shown beneath a password field.
   *
   * @return the hint, stating the actual minimum
   * @since 1.19.0
   */
  public static String hint() {
    return "Please provide a password with at least %d characters".formatted(MIN_LENGTH);
  }

  /**
   * Indicates whether the given password satisfies the length rule.
   *
   * <p>Provided so that view-level validators evaluate the rule with the same comparison the domain
   * uses ({@code >= MIN_LENGTH}). It is a convenience for feedback, not the authority — the domain
   * re-checks on the way in.
   *
   * @param password the raw password to inspect; {@code null} fails
   * @return {@code true} if the password is long enough
   * @since 1.19.0
   */
  public static boolean satisfiesMinLength(String password) {
    return password != null && password.length() >= MIN_LENGTH;
  }
}
