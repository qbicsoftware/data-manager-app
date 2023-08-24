package life.qbic.authentication.domain.user.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>User Information Service</b>
 *
 * <p>Application service within the user management context.
 * Used to retrieve basic user information within the data-manager application
 */
@Service
public class UserInformationService {

  private final UserRepository userRepository;
  private static final Logger log = LoggerFactory.logger(UserInformationService.class);

  public UserInformationService(@Autowired UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Optional<User> findById(UserId userId) {
    return userRepository.findById(userId);
  }

  public List<User> findAllActiveUsers() {
    return userRepository.findAllActiveUsers();
  }
}
