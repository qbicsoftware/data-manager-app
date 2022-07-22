package life.qbic.authentication.persistence;

import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.user.concept.EmailAddress;
import life.qbic.identity.domain.user.concept.User;
import life.qbic.identity.domain.user.repository.UserDataStorage;
import life.qbic.identity.domain.user.concept.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * <b>User JPA repository</b>
 *
 * <p>Implementation for the {@link UserDataStorage} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact with
 * persistent {@link User} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicUserRepo}, which is injected as dependency
 * upon creation.
 *
 * @since 1.0.0
 */
@Component
public class UserJpaRepository implements UserDataStorage {

  private final QbicUserRepo userRepo;

  @Autowired
  public UserJpaRepository(QbicUserRepo userRepo) {
    this.userRepo = userRepo;
  }

  @Override
  public List<User> findUsersByEmailAddress(EmailAddress emailAddress) {
    return userRepo.findUsersByEmailAddress(emailAddress);
  }

  @Override
  public void save(User user) {
    userRepo.save(user);
  }

  @Override
  public Optional<User> findUserById(UserId id) {
    return Optional.ofNullable(userRepo.findUserById(id));
  }
}
