package life.qbic.domain.usermanagement.login;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface LoginUserInput {

  void login(String email, String password);

  void setOutput(LoginUserOutput output);

}
