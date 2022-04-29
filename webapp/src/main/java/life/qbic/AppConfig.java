package life.qbic;

import life.qbic.usermanagement.persistence.QbicUserRepo;
import life.qbic.usermanagement.registration.RegisterUserInput;
import life.qbic.usermanagement.registration.RegisterUserOutput;
import life.qbic.usermanagement.registration.Registration;
import life.qbic.usermanagement.repository.UserDataStorage;
import life.qbic.usermanagement.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Configuration
public class AppConfig {

  @Bean
  public RegisterUserInput registerUserInput(UserRepository userRepository) {
    return new Registration(userRepository);
  }
  @Bean
  public UserRepository userRepository(UserDataStorage userDataStorage) {
    return UserRepository.getInstance(userDataStorage);
  }

}
