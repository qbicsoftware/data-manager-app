package life.qbic;

import life.qbic.apps.datamanager.events.EventStore;
import life.qbic.apps.datamanager.notifications.MessageBusInterface;
import life.qbic.apps.datamanager.notifications.NotificationService;
import life.qbic.apps.datamanager.services.UserRegistrationService;
import life.qbic.domain.usermanagement.registration.RegisterUserInput;
import life.qbic.domain.usermanagement.registration.Registration;
import life.qbic.domain.usermanagement.repository.UserDataStorage;
import life.qbic.domain.usermanagement.repository.UserRepository;
import life.qbic.events.SimpleEventStore;
import life.qbic.events.TemporaryEventRepository;
import life.qbic.messaging.Exchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <b>App bean configuration class</b>
 * <p>
 * Not all components can be generated on the fly by Spring, some we have to call explicitly via
 * factory methods.
 *
 * @since 1.0.0
 */
@Configuration
public class AppConfig {

  /**
   * Creates the registration use case.
   *
   * @param userRepository the user repository
   * @return the use case input
   * @since 1.0.0
   */
  @Bean
  public RegisterUserInput registerUserInput(UserRegistrationService userRegistrationService) {
    return new Registration(userRegistrationService);
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
  public UserRegistrationService userRegistrationService(NotificationService notificationService, EventStore eventStore) {
    return new UserRegistrationService(notificationService, eventStore);
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
  public MessageBusInterface messageBusInterface() {
    return Exchange.instance();
  }



}
