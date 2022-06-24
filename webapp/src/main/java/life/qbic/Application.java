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
import life.qbic.identityaccess.application.user.PasswordResetOutput;
import life.qbic.identityaccess.application.user.PasswordResetRequest;
import life.qbic.identityaccess.domain.DomainRegistry;
import life.qbic.identityaccess.domain.user.PasswordReset;
import life.qbic.identityaccess.domain.user.UserDomainService;
import life.qbic.identityaccess.domain.user.UserRegistered;
import life.qbic.identityaccess.domain.user.UserRepository;
import life.qbic.shared.application.notification.MessageBusInterface;
import life.qbic.shared.application.notification.MessageSubscriber;
import life.qbic.usermanagement.EmailFactory;
import life.qbic.usermanagement.passwordreset.PasswordResetLinkSupplier;
import life.qbic.usermanagement.registration.EmailConfirmationLinkSupplier;
import life.qbic.views.login.LoginHandler;
import life.qbic.views.login.resetPassword.PasswordResetHandler;
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
  private static final String qbicNoReply = "no-reply@qbic.life";

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

    var passwordReset = context.getBean(PasswordResetRequest.class);
    var passwordResetHandler = (PasswordResetOutput) context.getBean(PasswordResetHandler.class);
    passwordReset.setUseCaseOutput(passwordResetHandler);
  }

  private static MessageSubscriber whenUserRegisteredSendEmail(
      ConfigurableApplicationContext appContext) {
    return (message, messageParams) -> {
      if (!messageParams.messageType.equals("UserRegistered")) {
        return;
      }
      try {
        UserRegistered userRegistered = deserializeUserRegistered(message);
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
    };
  }

  private static MessageSubscriber whenPasswordResetRequestSendEmail(
      ConfigurableApplicationContext appContext) {
    return (message, messageParams) -> {
      if (!messageParams.messageType.equals("PasswordReset")) {
        return;
      }
      try {
        var passwordResetRequest = deserializePasswordReset(message);
        var passwordResetLink = appContext.getBean(PasswordResetLinkSupplier.class)
            .passwordResetUrl(passwordResetRequest.userId().get());
        var registrationEmailSender = appContext.getBean(
            EmailService.class);
        var passwordResetEmail = EmailFactory.passwordResetEmail(qbicNoReply,
            new Recipient(passwordResetRequest.userEmailAddress().get(),
                passwordResetRequest.userFullName().get())
            , passwordResetLink);
        registrationEmailSender.send(passwordResetEmail);
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private static MessageSubscriber whenUserRegisteredLogUserInfo() {
    return (message, messageParams) -> {
      try {
        UserRegistered userRegistered = deserializeUserRegistered(message);
        log.info(String.valueOf(userRegistered));
      } catch (IOException | ClassNotFoundException e) {
        log.error(e.getMessage(), e);
      }
    };
  }

  static UserRegistered deserializeUserRegistered(String event)
      throws IOException, ClassNotFoundException {
    byte[] content = Base64.decode(event);
    ByteArrayInputStream bais = new ByteArrayInputStream(content);
    ObjectInputStream ois = new ObjectInputStream(bais);
    return (UserRegistered) ois.readObject();
  }

  static PasswordReset deserializePasswordReset(String event)
      throws IOException, ClassNotFoundException {
    byte[] content = Base64.decode(event);
    ByteArrayInputStream bais = new ByteArrayInputStream(content);
    ObjectInputStream ois = new ObjectInputStream(bais);
    return (PasswordReset) ois.readObject();
  }
}
