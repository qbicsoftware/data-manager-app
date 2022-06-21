package life.qbic;

import static org.slf4j.LoggerFactory.getLogger;

import com.helger.commons.base64.Base64;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import life.qbic.email.Email;
import life.qbic.email.EmailService;
import life.qbic.email.Recipient;
import life.qbic.identityaccess.application.user.ConfirmEmailOutput;
import life.qbic.identityaccess.application.user.EmailAddressConfirmation;
import life.qbic.identityaccess.domain.DomainRegistry;
import life.qbic.identityaccess.domain.user.UserDomainService;
import life.qbic.identityaccess.domain.user.UserRegistered;
import life.qbic.identityaccess.domain.user.UserRepository;
import life.qbic.shared.application.notification.MessageBusInterface;
import life.qbic.shared.application.notification.MessageSubscriber;
import life.qbic.usermanagement.EmailFactory;
import life.qbic.usermanagement.registration.EmailConfirmationLinkSupplier;
import life.qbic.views.login.LoginHandler;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The entry point of the Spring Boot application.
 *
 * <p>Use the @PWA annotation make the application installable on phones, tablets and some desktop
 * browsers.
 */
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@Theme(value = "datamanager")
@PWA(
    name = "Data Manager",
    shortName = "Data Manager",
    offlineResources = {"images/logo.png"})
@NpmPackage(value = "line-awesome", version = "1.3.0")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

  private static final Logger log = getLogger(Application.class);

  @Serial
  private static final long serialVersionUID = -8182104817961102407L;


  public static void main(String[] args) {
    var appContext = SpringApplication.run(Application.class, args);

    // We need to set up the domain registry and register important services:
    var userRepository = appContext.getBean(UserRepository.class);
    DomainRegistry.instance().registerService(new UserDomainService(userRepository));

    var messageBus = appContext.getBean(MessageBusInterface.class);
    messageBus.subscribe(whenUserRegisteredSendEmail(appContext), "UserRegistered");
    messageBus.subscribe(whenUserRegisteredLogUserInfo(), "UserRegistered");
    messageBus.subscribe(whenPasswordResetRequestSendEmail(appContext), "PasswordReset");

    setupUseCases(appContext);
  }

  private static void setupUseCases(ConfigurableApplicationContext context) {
    var emailAddressConfirmation = context.getBean(EmailAddressConfirmation.class);
    var loginHandler = (ConfirmEmailOutput) context.getBean(LoginHandler.class);
    emailAddressConfirmation.setConfirmEmailOutput(loginHandler);
  }

  private static void sendEmail(ConfigurableApplicationContext appContext, String message) {
    try {
      UserRegistered userRegistered = deserialize(message);
      String emailConfirmationUrl = appContext.getBean(EmailConfirmationLinkSupplier.class)
          .emailConfirmationUrl(userRegistered.userId());
      EmailService registrationEmailSender = appContext.getBean(
          EmailService.class);
      Email registrationMail = EmailFactory.registrationEmail("no-reply@qbic.life",
          new Recipient(userRegistered.userEmail(), userRegistered.userFullName())
          , emailConfirmationUrl);
      registrationEmailSender.send(registrationMail);
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static MessageSubscriber whenUserRegisteredSendEmail(
      ConfigurableApplicationContext appContext) {
    return (message, messageParams) -> {
      if (!messageParams.messageType.equals("UserRegistered")) {
        return;
      }
      sendEmail(appContext, message);
    };
  }

  private static MessageSubscriber whenPasswordResetRequestSendEmail(
      ConfigurableApplicationContext appContext) {
    return (message, messageParams) -> {
      if (!messageParams.messageType.equals("PasswordReset")) {
        return;
      }
      sendEmail(appContext, message);
    };
  }

  private static MessageSubscriber whenUserRegisteredLogUserInfo() {
    return (message, messageParams) -> {
      try {
        UserRegistered userRegistered = deserialize(message);
        log.info(String.valueOf(userRegistered));
      } catch (IOException | ClassNotFoundException e) {
        log.error(e.getMessage(), e);
      }
    };
  }

  static UserRegistered deserialize(String event) throws IOException, ClassNotFoundException {
    byte[] content = Base64.decode(event);
    ByteArrayInputStream bais = new ByteArrayInputStream(content);
    ObjectInputStream ois = new ObjectInputStream(bais);
    return (UserRegistered) ois.readObject();
  }
}
