package life.qbic.datamanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Base64;
import life.qbic.broadcasting.MessageSubscriber;
import life.qbic.broadcasting.MessageSubscription;
import life.qbic.datamanager.views.login.LoginHandler;
import life.qbic.datamanager.views.login.newpassword.NewPasswordHandler;
import life.qbic.datamanager.views.login.passwordreset.PasswordResetHandler;
import life.qbic.identity.application.communication.broadcasting.IntegrationEvent;
import life.qbic.identity.application.user.password.NewPassword;
import life.qbic.identity.application.user.password.NewPasswordOutput;
import life.qbic.identity.application.user.password.PasswordResetOutput;
import life.qbic.identity.application.user.password.PasswordResetRequest;
import life.qbic.identity.application.user.registration.ConfirmEmailOutput;
import life.qbic.identity.application.user.registration.EmailAddressConfirmation;
import life.qbic.identity.domain.event.PasswordResetRequested;
import life.qbic.identity.domain.event.UserRegistered;
import life.qbic.identity.domain.registry.DomainRegistry;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.identity.domain.service.UserDomainService;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.infrastructure.project.access.QBiCSid;
import life.qbic.projectmanagement.infrastructure.project.access.SidRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.annotation.JmsListener;

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
@ComponentScan({"life.qbic"})
@EnableJpaRepositories(basePackages = "life.qbic")
@EntityScan(basePackages = "life.qbic")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

  private static final Logger logger = LoggerFactory.logger(Application.class.getName());

  @Serial
  private static final long serialVersionUID = -8182104817961102407L;

  public static void main(String[] args) {
    logger.info("Starting data manager app...");
    var appContext = SpringApplication.run(Application.class, args);

    // We need to set up the domain registry and register important services:
    var userRepository = appContext.getBean(UserRepository.class);
    DomainRegistry.instance().registerService(new UserDomainService(userRepository));

    setupUseCases(appContext);
  }

  private static void setupUseCases(ConfigurableApplicationContext context) {
    var emailAddressConfirmation = context.getBean(EmailAddressConfirmation.class);
    var loginHandler = (ConfirmEmailOutput) context.getBean(LoginHandler.class);
    emailAddressConfirmation.setConfirmEmailOutput(loginHandler);

    var passwordReset = context.getBean(PasswordResetRequest.class);
    var passwordResetHandler = (PasswordResetOutput) context.getBean(PasswordResetHandler.class);
    passwordReset.setUseCaseOutput(passwordResetHandler);

    var newPassword = context.getBean(NewPassword.class);
    var newPasswordHandler = (NewPasswordOutput) context.getBean(NewPasswordHandler.class);
    newPassword.setUseCaseOutput(newPasswordHandler);
  }
}
