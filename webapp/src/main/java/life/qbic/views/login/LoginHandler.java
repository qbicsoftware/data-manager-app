package life.qbic.views.login;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
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
  public boolean handle(LoginLayout loginView) {
    if (registeredLoginView != loginView) {
      registeredLoginView = loginView;
      // orchestrate view
      addListener();
      // then return
      return true;
    }

    return false;
  }

  private void addListener() {
    registeredLoginView.loginButton.addClickShortcut(Key.ENTER);
    registeredLoginView.loginButton.getElement().setProperty("action", "login");

    registeredLoginView.loginButton.addClickListener(
        event -> {
              UI.getCurrent().navigate("about"); // could be dashboard later
        });
  }
}
