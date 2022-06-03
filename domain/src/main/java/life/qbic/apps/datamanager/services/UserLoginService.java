package life.qbic.apps.datamanager.services;

import java.util.Optional;
import life.qbic.domain.usermanagement.User;
import life.qbic.domain.usermanagement.repository.UserRepository;

/**
 * <b>User Login Service</b>
 *
 * <p>This service facilitates user login.</p>
 *
 * @since 1.0.0
 */
public final class UserLoginService {

  private final UserRepository userRepository;

  public UserLoginService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public boolean login(String email, String password) {
    Optional<User> user = userRepository.findByEmail(email);
    return user
        .map(it -> it.checkPassword(password.toCharArray()))
        .orElse(false);
  }

}
