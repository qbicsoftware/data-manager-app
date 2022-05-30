package life.qbic.domain.usermanagement.registration;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface ConfirmEmailOutput {

  void onSuccess();

  void onFailure(String reason);

}
