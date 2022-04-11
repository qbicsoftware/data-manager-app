package life.qbic.usermanagement.repository;

import java.util.Optional;
import life.qbic.usermanagement.User;

public class UserRepository {

  private static UserRepository INSTANCE;

  private final UserDataStorage dataStorage;

  public static UserRepository getInstance(UserDataStorage dataStorage) {
    if (INSTANCE == null) {
      INSTANCE = new UserRepository(dataStorage);
    }
    return INSTANCE;
  }
  protected UserRepository(UserDataStorage dataStorage) {
    this.dataStorage = dataStorage;
  }

  public Optional<User> findByEmail(String email) {
    return Optional.empty();
  }

  public boolean addUser(User user) {
    return false;
  }

}
