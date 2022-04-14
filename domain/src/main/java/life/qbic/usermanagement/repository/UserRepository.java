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
    var matchingUsers = dataStorage.findUsersByEmail(email);
    if (matchingUsers.size() > 1) {
      throw new RuntimeException("More than one user entry with the same email exists!");
    }
    if (matchingUsers.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(matchingUsers.get(0));
  }

  public Optional<User> findById(String id) {
    return dataStorage.findUserById(id);
  }

  public boolean addUser(User user) {
    Optional<User> userSearch = findById(user.getId());
    if (userSearch.isPresent()) {
      return false;
    }
    return dataStorage.storeUser(user);
  }

}
