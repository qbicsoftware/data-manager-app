package life.qbic.datamanager;

import life.qbic.broadcasting.Exchange;
import life.qbic.newshandler.usermanagement.email.EmailService;
import life.qbic.authentication.domain.event.SimpleEventStore;
import life.qbic.authentication.domain.event.TemporaryEventRepository;
import life.qbic.authentication.domain.user.event.EventStore;
import life.qbic.broadcasting.MessageBusSubmission;
import life.qbic.authentication.application.notification.NotificationService;
import life.qbic.authentication.application.user.registration.EmailAddressConfirmation;
import life.qbic.authentication.application.user.password.NewPassword;
import life.qbic.authentication.application.user.password.NewPasswordInput;
import life.qbic.authentication.application.user.password.PasswordResetInput;
import life.qbic.authentication.application.user.password.PasswordResetRequest;
import life.qbic.authentication.application.user.registration.RegisterUserInput;
import life.qbic.authentication.application.user.registration.Registration;
import life.qbic.authentication.application.user.registration.UserRegistrationService;
import life.qbic.authentication.domain.user.repository.UserDataStorage;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.newshandler.usermanagement.email.EmailSubmissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * <b>App bean configuration class</b>
 *
 * <p>Not all components can be generated on the fly by Spring, some we have to call explicitly via
 * factory methods.
 *
 * @since 1.0.0
 */
@Configuration
@ComponentScan({"life.qbic.authentication.persistence"})
public class AppConfig {

  /**
   * Creates the registration use case.
   *
   * @param userRegistrationService the user registration services used by this use case
   * @return the use case input
   * @since 1.0.0
   */
  @Bean
  public RegisterUserInput registerUserInput(UserRegistrationService userRegistrationService) {
    return new Registration(userRegistrationService);
  }

  @Bean
  public EmailAddressConfirmation confirmEmailInput(
      UserRegistrationService userRegistrationService) {
    return new EmailAddressConfirmation(userRegistrationService);
  }

  /**
   * Creates the user repository instance.
   *
   * @param userDataStorage an implementation of the {@link UserDataStorage} interface
   * @return a Singleton of the user repository
   * @since 1.0.0
   */
  @Bean
  public UserRepository userRepository(UserDataStorage userDataStorage) {
    return UserRepository.getInstance(userDataStorage);
  }

  @Bean
  public UserRegistrationService userRegistrationService(
      NotificationService notificationService, UserRepository userRepository,
      EventStore eventStore) {
    return new UserRegistrationService(notificationService, userRepository, eventStore);
  }


  @Bean
  public SimpleEventStore eventStore() {
    return SimpleEventStore.instance(new TemporaryEventRepository());
  }

  @Bean
  public NotificationService notificationService(MessageBusSubmission messageBusInterface) {
    return new NotificationService(messageBusInterface);
  }

  @Bean
  public EmailService emailService() {
    return new EmailSubmissionService();
  }

  @Bean
  public MessageBusSubmission messageBusInterface() {
    return Exchange.instance();
  }

  @Bean
  public PasswordResetInput passwordResetInput(UserRegistrationService userRegistrationService) {
    return new PasswordResetRequest(userRegistrationService);
  }

  @Bean
  public NewPasswordInput newPasswordInput(UserRegistrationService userRegistrationService) {
    return new NewPassword(userRegistrationService);
  }
}
