package life.qbic.datamanager.views.login;

import java.util.Objects;
import life.qbic.datamanager.Application;
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
