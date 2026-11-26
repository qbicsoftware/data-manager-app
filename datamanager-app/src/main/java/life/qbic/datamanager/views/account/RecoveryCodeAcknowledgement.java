package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.server.VaadinSession;

/**
 * <b>Whether a one-time recovery-code sheet was closed without being acknowledged (MFA-91)</b>
 *
 * <p>The codes exist only on that sheet; only their hashes are stored, so they can never be shown
 * again. A refresh or a navigation discards the sheet, and the user is left believing they can look
 * the codes up later. The application cannot repair that, but it can refuse to be silent — this marker
 * is how the settings page knows to say so.</p>
 *
 * <p><b>Set before the sheet opens, cleared only by the acknowledgement.</b> Every other exit leaves
 * it standing, which is the whole point: "was it saved?" is not a question the database can answer.</p>
 *
 * <p><b>In the session, not in a component.</b> The component does not survive the refresh that causes
 * the loss, so a field on the page would be gone by the time the page renders again.</p>
 *
 * <p>Shared by both entry points — the settings page and the enrolment page — so a lost sheet is
 * reported the same way regardless of where it was opened.</p>
 *
 * @since 1.0.0
 */
public final class RecoveryCodeAcknowledgement {

  /**
   * Session key. Public so a spec can assert on it without duplicating the literal.
   */
  public static final String UNACKNOWLEDGED_ATTRIBUTE = "qbic.mfa.recovery-codes-unacknowledged";

  private RecoveryCodeAcknowledgement() {
  }

  /**
   * Records that a sheet was shown and not (yet) acknowledged.
   *
   * @param userId       the account the sheet belongs to
   * @param unacknowledged {@code true} when the sheet is open, {@code false} once acknowledged
   */
  public static void markUnacknowledged(String userId, boolean unacknowledged) {
    requireNonNull(userId, "userId must not be null");
    VaadinSession session = VaadinSession.getCurrent();
    if (session == null) {
      return;
    }
    session.setAttribute(UNACKNOWLEDGED_ATTRIBUTE, unacknowledged ? Boolean.TRUE : null);
  }

  /**
   * @return {@code true} if a sheet was shown and never acknowledged in this session
   */
  public static boolean isUnacknowledged() {
    VaadinSession session = VaadinSession.getCurrent();
    return session != null && Boolean.TRUE.equals(session.getAttribute(UNACKNOWLEDGED_ATTRIBUTE));
  }
}
