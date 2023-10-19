package life.qbic.identity.infrastructure;

import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.projectmanagement.infrastructure.QBiCSid;
import life.qbic.projectmanagement.infrastructure.SidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * <b>User JPA repository</b>
 *
 * <p>Implementation for the {@link UserDataStorage} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with
 * persistent {@link User} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicUserRepo}, which is injected as
 * dependency
 * upon creation.
 *
 * @since 1.0.0
 */
@Component
public class UserJpaRepository implements UserDataStorage, SidDataStorage {

  private final QbicUserRepo userRepo;
  private final SidRepository sidRepository;

  @Autowired
  public UserJpaRepository(QbicUserRepo userRepo, SidRepository sidRepository) {
    this.userRepo = userRepo;
    this.sidRepository = sidRepository;
  }

  @Override
  public List<User> findUsersByEmailAddress(EmailAddress emailAddress) {
    return userRepo.findUsersByEmailAddress(emailAddress);
  }

  @Override
  public void save(User user) {
    userRepo.save(user);
    if (!sidRepository.existsBySidEqualsIgnoreCaseAndPrincipalEquals(user.emailAddress().get(),
        true)) {
      addSid(user.id().get(), true);
    }
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
  public void addSid(String sid, boolean principal) {
    sidRepository.save(new QBiCSid(principal, sid));
  }
}
