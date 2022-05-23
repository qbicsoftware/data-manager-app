package life.qbic;

import com.helger.commons.base64.Base64;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import life.qbic.apps.datamanager.notifications.MessageBusInterface;
import life.qbic.apps.datamanager.notifications.MessageSubscriber;
import life.qbic.domain.usermanagement.DomainRegistry;
import life.qbic.domain.usermanagement.UserDomainService;
import life.qbic.domain.usermanagement.registration.UserRegistered;
import life.qbic.domain.usermanagement.repository.UserRepository;
import life.qbic.usermanagement.registration.RegistrationEmailSender;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

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

  public static void main(String[] args) {
    var appContext = SpringApplication.run(Application.class, args);

    // We need to set up the domain registry and register important services:
    var userRepository = appContext.getBean(UserRepository.class);
    DomainRegistry.instance().registerService(new UserDomainService(userRepository));

    // Testing: subscribe to user register events in the message bus
    MessageSubscriber whenUserRegisteredSendEmail = (message, messageParams) -> {
      if (!messageParams.messageType.equals("UserRegistered")) {
        return;
      }
      try {
        UserRegistered userRegistered = deserialize(message);
        RegistrationEmailSender registrationEmailSender = appContext.getBean(
            RegistrationEmailSender.class);
        registrationEmailSender.sendmail(userRegistered.userEmail(), userRegistered.userFullName());
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    };

    var messageBus = appContext.getBean(MessageBusInterface.class);
    messageBus.subscribe(whenUserRegisteredSendEmail, "UserRegistered");
  }

  static UserRegistered deserialize(String event) throws IOException, ClassNotFoundException {
    byte[] content = Base64.decode(event);
    ByteArrayInputStream bais = new ByteArrayInputStream(content);
    ObjectInputStream ois = new ObjectInputStream(bais);
    return (UserRegistered) ois.readObject();
  }
}
