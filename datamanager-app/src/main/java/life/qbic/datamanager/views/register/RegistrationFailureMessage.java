package life.qbic.datamanager.views.register;

import life.qbic.identity.api.UserNames;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.identity.application.user.IdentityService.UserNameTooLongException;

/**
 * <b>The one place a registration failure is turned into text.</b>
 *
 * <p>{@link UserRegistrationMain} and {@link RegistrationOrcIdMain} each carried their own copy of the
 * same {@code handleRegistrationFailure} switch: the same three user-name cases, the same wording, the
 * same default. The copies had to be edited in step, and the email case had already been removed from
 * both by hand when {@code AUTH-NFR-03} forbade it — exactly the kind of manual synchronisation that
 * fails silently.</p>
 *
 * <p><b>There is no "email already in use" case, and there must never be one.</b> That sentence is the
 * account-existence oracle. A duplicate registration is answered uniformly by
 * {@link RegistrationSubmission} and never reaches this class; the branch was removed rather than left
 * unreachable so it cannot be restored by accident while looking like a helpful default.</p>
 *
 * @since 1.20.2
 */
public record RegistrationFailureMessage(String title, String description) {

  /**
   * Maps one failure to the text the form renders.
   *
   * @param failure the failure returned by {@link RegistrationSubmission}
   * @return the title and description for the form's notification area
   */
  public static RegistrationFailureMessage of(RuntimeException failure) {
    if (failure instanceof UserNameNotAvailableException) {
      return new RegistrationFailureMessage("Username already in use",
          "Please try another username");
    }
    if (failure instanceof EmptyUserNameException) {
      return new RegistrationFailureMessage("Username must not be empty",
          "Please try another username");
    }
    if (failure instanceof UserNameTooLongException) {
      return new RegistrationFailureMessage("Username is too long",
          "User name must not exceed " + UserNames.MAX_LENGTH + " characters");
    }
    return new RegistrationFailureMessage("Registration failed", "Please try again.");
  }

  /**
   * Renders a failure on the merged registration form.
   *
   * @param form    the form to render on
   * @param failure the failure to describe
   */
  public static void render(UserRegistrationComponent form, RuntimeException failure) {
    RegistrationFailureMessage message = of(failure);
    form.showError(message.title(), message.description());
  }
}
