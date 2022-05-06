package life.qbic.views.login;

import life.qbic.usermanagement.persistence.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b> The LoginHandler handles the view elements of the {@link LoginLayout}. </b>
 *
 * @since 1.0.0
 */
@Component
public class LoginHandler implements LoginHandlerInterface {

  private final UserJpaRepository userRepository;

  private LoginLayout registeredLoginView;

  LoginHandler(@Autowired UserJpaRepository repository) {
    this.userRepository = repository;
  }

  @Override
  public void handle(LoginLayout loginView) {
    if (registeredLoginView != loginView) {
      registeredLoginView = loginView;
    }
  }

}
