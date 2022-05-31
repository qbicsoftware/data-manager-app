package life.qbic.domain.usermanagement.registration;

/**
 * <b>Confirm Email use case output</b>
 *
 * @since 1.0.0
 */
public interface ConfirmEmailOutput {

  void onSuccess();

  void onFailure(String reason);

}
