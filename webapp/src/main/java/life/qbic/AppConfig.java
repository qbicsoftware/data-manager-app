package life.qbic;

import life.qbic.events.EventStore;
import life.qbic.events.TemporaryEventRepository;
import life.qbic.usermanagement.registration.RegisterUserInput;
import life.qbic.usermanagement.registration.Registration;
import life.qbic.usermanagement.repository.UserDataStorage;
import life.qbic.usermanagement.repository.UserRepository;
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
  public RegisterUserInput registerUserInput(UserRepository userRepository) {
    return new Registration(userRepository);
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

  /**
   * Creates the event store instance
   * @return 1.0.0
   */
  @Bean
  public EventStore eventStore() {
    return EventStore.instance(new TemporaryEventRepository());
  }

}
