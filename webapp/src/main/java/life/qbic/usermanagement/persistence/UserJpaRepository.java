package life.qbic.usermanagement.persistence;

import java.util.List;
import java.util.Optional;
import life.qbic.usermanagement.User;
import life.qbic.usermanagement.repository.UserDataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class UserJpaRepository implements UserDataStorage {

  private final QbicUserRepo userRepo;

  @Autowired
  public UserJpaRepository(QbicUserRepo userRepo) {
    this.userRepo = userRepo;
  }

  @Override
  public List<User> findUsersByEmail(String email) {
    return userRepo.findUsersByEmail(email);
  }

  @Override
  public void storeUser(User user) {
    userRepo.save(user);
  }

  @Override
  public Optional<User> findUserById(String id) {
    return Optional.ofNullable(userRepo.findUserById(id));
  }
}
