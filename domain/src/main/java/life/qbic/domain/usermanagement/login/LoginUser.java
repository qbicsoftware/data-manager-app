package life.qbic.domain.usermanagement.login;

import life.qbic.apps.datamanager.services.UserLoginService;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
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
