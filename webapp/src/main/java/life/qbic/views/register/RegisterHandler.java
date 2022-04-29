package life.qbic.views.register;

import com.vaadin.flow.component.UI;
import life.qbic.usermanagement.User;
import life.qbic.usermanagement.persistence.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles the {@link RegisterLayout} components
 *
 * <p>This class is responsible for enabling buttons or triggering other view relevant changes on
 * the view class components
 */
@Component
public class RegisterHandler implements RegisterHandlerInterface {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger(RegisterHandler.class);

  private final UserJpaRepository userJpaRepository;
  private RegisterLayout registeredRegisterLayout;

  RegisterHandler(UserJpaRepository userJpaRepository) {
    this.userJpaRepository = userJpaRepository;
  }

  @Override
  public boolean register(RegisterLayout registerLayout) {
    if (registeredRegisterLayout != registerLayout) {
      this.registeredRegisterLayout = registerLayout;
      // orchestrate view
      addListener();
      // then return
      return true;
    }
    return false;
  }

  private void addListener() {
    registeredRegisterLayout.registerButton.addClickListener(
        event -> {
          List<User> user =
              userJpaRepository.findUsersByEmail(registeredRegisterLayout.email.getValue());

          if (user.size() > 0) {
            registeredRegisterLayout.alreadyUsedEmailMessage.setVisible(true);
          } else {
            registeredRegisterLayout.alreadyUsedEmailMessage.setVisible(false);
            try {
              String password = registeredRegisterLayout.password.getValue();
              String email = registeredRegisterLayout.email.getValue();
              String name = registeredRegisterLayout.fullName.getValue();

              User newUser = User.create(password, name, email);
              // todo validate the password length with the policy (use case)
              // and then show the error message if too short
              // registeredRegisterLayout.passwordTooShortMessage.setVisible(true); also hide it
              // again
              // also check if email is valid similar the above
              userJpaRepository.save(newUser);
              UI.getCurrent().navigate("login");
            } catch (Exception e) {
              log.error(e.getMessage());
              registeredRegisterLayout.errorMessage.setVisible(true);
            }
          }
        });
  }
}
