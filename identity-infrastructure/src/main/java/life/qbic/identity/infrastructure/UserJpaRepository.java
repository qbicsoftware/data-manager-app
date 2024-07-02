package life.qbic.identity.infrastructure;

import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserDataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;


/**
 * <b>User JPA repository</b>
 *
 * <p>Implementation for the {@link UserDataStorage} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link User} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicUserRepo}, which is injected as
 * dependency upon creation.
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

  @Override
  public List<User> findAllActiveUsers() {
    return userRepo.findUsersByActiveTrue();
  }

  @Override
  public Optional<User> findUserByUserName(String userName) {
    return Optional.ofNullable(userRepo.findUserByUserName(userName));
  }

  @Override
  public List<User> findByUserNameContainingIgnoreCaseAndActiveTrue(String userName,
      Pageable pageable) {
    return userRepo.findAllByUserNameContainingIgnoreCaseAndActiveTrue(userName, pageable);
  }

  @Override
  public Optional<User> findByOidcIdEqualsAndOidcIssuerEquals(String oidcId, String oidcIssuer) {
    return userRepo.findByOidcIdEqualsAndOidcIssuerEquals(oidcId, oidcIssuer);
  }
}
