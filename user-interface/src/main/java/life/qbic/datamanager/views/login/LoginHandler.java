package life.qbic.datamanager.views.login;

import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.datamanager.Application;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.identity.application.user.registration.ConfirmEmailInput;
import life.qbic.identity.application.user.registration.ConfirmEmailOutput;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b> The LoginHandler handles the view elements of the {@link LoginLayout}. </b>
 *
 * @since 1.0.0
 */
@Component
public class LoginHandler {

  private static final Logger logger = LoggerFactory.logger(Application.class.getName());


  private final String emailConfirmationParameter;

  @Autowired
  LoginHandler(@Value("${EMAIL_CONFIRMATION_PARAMETER:confirm-email}") String emailConfirmationParameter) {
    this.emailConfirmationParameter = Objects.requireNonNull(emailConfirmationParameter);
  }

  public String emailConfirmationParameter() {
    return emailConfirmationParameter;
  }

}
