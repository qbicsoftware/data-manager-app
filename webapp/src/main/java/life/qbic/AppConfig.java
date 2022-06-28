package life.qbic;

import life.qbic.identityaccess.application.user.NewPassword;
import life.qbic.identityaccess.application.user.NewPasswordInput;
import life.qbic.identityaccess.application.user.PasswordResetInput;
import life.qbic.identityaccess.application.user.PasswordResetRequest;
import life.qbic.shared.application.notification.EventStore;
import life.qbic.email.EmailService;
import life.qbic.events.SimpleEventStore;
import life.qbic.events.TemporaryEventRepository;
import life.qbic.shared.application.notification.MessageBusInterface;
import life.qbic.shared.application.notification.NotificationService;
import life.qbic.identityaccess.application.user.EmailAddressConfirmation;
import life.qbic.identityaccess.application.user.RegisterUserInput;
import life.qbic.identityaccess.application.user.Registration;
import life.qbic.identityaccess.application.user.UserRegistrationService;
import life.qbic.identityaccess.domain.user.UserDataStorage;
import life.qbic.identityaccess.domain.user.UserRepository;
import life.qbic.messaging.Exchange;
import life.qbic.usermanagement.EmailSubmissionService;
import org.springframework.context.annotation.Bean;
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
public class AppConfig {

  /**
   * Creates the registration use case.
   *
   * @param userRegistrationService the user registration service used by this use case
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
  public NotificationService notificationService(MessageBusInterface messageBusInterface) {
    return new NotificationService(messageBusInterface);
  }

  @Bean
  public EmailService emailService() {
    return new EmailSubmissionService();
  }

  @Bean
  public MessageBusInterface messageBusInterface() {
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
