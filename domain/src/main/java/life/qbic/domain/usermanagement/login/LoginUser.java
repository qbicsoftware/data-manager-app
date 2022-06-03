package life.qbic.domain.usermanagement.login;

import life.qbic.apps.datamanager.services.UserLoginService;

/**
 * <b>Login User use case</b>
 *
 * <p>Tries to login an existing user.
 *
 * <p>In case a user with the provided email does not exist exists or the credentials are wrong,
 * the
 * login will fail and calls the failure output method.
 *
 * @since 1.0.0
 */
public class LoginUser implements LoginUserInput {

  private final UserLoginService userLoginService;
  private LoginUserOutput output;

  public LoginUser(UserLoginService userLoginService) {
    this.userLoginService = userLoginService;
  }

  @Override
  public void login(String email, String password) {
    boolean loginSuccessful = userLoginService.login(email, password);
    if (loginSuccessful) {
      output.onLoginSucceeded();
    } else {
      output.onLoginFailed();
    }
  }

  @Override
  public void setOutput(LoginUserOutput output) {
    this.output = output;
  }
}
