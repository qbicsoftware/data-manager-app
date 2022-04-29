package life.qbic.usermanagement.registration;

public interface RegisterUserOutput {

  void onSuccess();

  void onFailure(String reason);

}
