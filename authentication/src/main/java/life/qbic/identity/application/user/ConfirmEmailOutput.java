package life.qbic.identity.application.user;

/**
 * <b>Confirm Email use case output</b>
 *
 * @since 1.0.0
 */
public interface ConfirmEmailOutput {

  void onEmailConfirmationSuccess();

  void onEmailConfirmationFailure(String reason);

}
