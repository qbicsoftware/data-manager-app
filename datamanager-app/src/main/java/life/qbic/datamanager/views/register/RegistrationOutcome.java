package life.qbic.datamanager.views.register;

import java.util.List;
import life.qbic.identity.application.user.IdentityService.UserExistsException;

/**
 * <b>What a registration submission should do, given the domain's failures.</b>
 *
 * <p>The two registration views (local and ORCID) each need the same decision, and before this class
 * each carried its own copy of the same {@code instanceof} chain. That duplication is how the
 * account-existence disclosure survived in two places at once: one copy was fixed, the other was not,
 * and the oracle simply moved.</p>
 *
 * <p>The decision is expressed as a pure function of the failure list — no services, no session, no
 * database — so it can be asserted directly. The views keep the side effects (notify, navigate), which
 * is what made them awkward to test.</p>
 *
 * @since 1.19.0
 */
public final class RegistrationOutcome {

  /**
   * The action a view must take for one submission.
   */
  public enum Action {
    /**
     * Treat the submission exactly like a successful registration: send the visitor to the "check your
     * inbox" page and disclose nothing. The address owner is notified separately.
     */
    UNIFORM_SUCCESS,
    /**
     * Render the failure. Only used for faults that reveal nothing about account existence —
     * {@code AUTH-NFR-03} allows telling a visitor their <em>input</em> was rejected, never that an
     * <em>account</em> exists.
     */
    REPORT_FAILURE
  }

  private RegistrationOutcome() {
    // utility class
  }

  /**
   * Decides what a view should do.
   *
   * <p>{@link UserExistsException} maps to {@link Action#UNIFORM_SUCCESS}. That is the whole point: a
   * duplicate registration must be indistinguishable from a successful one, so the view must not
   * render "email already in use", and it must not navigate anywhere a new registration would not
   * reach.</p>
   *
   * <p>Every other failure keeps its existing handling. A taken username, for instance, is reportable:
   * it does not answer whether an <em>account</em> exists, and suppressing it would leave the visitor
   * stuck with no explanation and no way forward.</p>
   *
   * @param failures the domain failures, possibly empty
   * @return the action to take; never {@code null}
   */
  public static Action decide(List<RuntimeException> failures) {
    if (failures == null || failures.isEmpty()) {
      // Nothing to report means the submission succeeded.
      return Action.UNIFORM_SUCCESS;
    }
    return isDuplicateAccount(failures) ? Action.UNIFORM_SUCCESS : Action.REPORT_FAILURE;
  }

  /**
   * Whether the failures say "an account already exists for this address".
   *
   * <p>Matched on the exception type rather than on rendered text, so the uniform response cannot be
   * defeated by a message change elsewhere.</p>
   *
   * @param failures the domain failures
   * @return {@code true} if the address is already registered
   */
  public static boolean isDuplicateAccount(List<RuntimeException> failures) {
    return failures != null
        && failures.stream().anyMatch(UserExistsException.class::isInstance);
  }
}
