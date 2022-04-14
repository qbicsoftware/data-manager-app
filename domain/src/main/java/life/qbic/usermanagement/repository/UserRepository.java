package life.qbic.usermanagement.repository;

import java.util.Optional;
import life.qbic.usermanagement.User;

public class UserRepository {

  private static UserRepository INSTANCE;

  private final UserDataStorage dataStorage;

  /**
   *
   * @param dataStorage
   * @return
   * @since
   */
  public static UserRepository getInstance(UserDataStorage dataStorage) {
    if (INSTANCE == null) {
      INSTANCE = new UserRepository(dataStorage);
    }
    return INSTANCE;
  }

  protected UserRepository(UserDataStorage dataStorage) {
    this.dataStorage = dataStorage;
  }

  /**
   * Searches for a user with the provided email address.
   * <p>
   * Note: A runtime exception is thrown, when there is more than one user found. We want the email
   * addresses to be unique in the user context, but they might change over time. So emails are not
   * suitable as entity identifiers but still need to be unique in the user management context at
   * any given time.
   * <p>
   * However, an email address of a user is not guaranteed to be persistent over time.
   *
   * @param email the email to find a matching user entry for
   * @return the user object wrapped in an {@link Optional} if found, otherwise returns {@link Optional#empty()}
   * @throws RuntimeException if there is more than one user matching the email address
   * @since 1.0.0
   */
  public Optional<User> findByEmail(String email) throws RuntimeException {
    var matchingUsers = dataStorage.findUsersByEmail(email);
    if (matchingUsers.size() > 1) {
      throw new RuntimeException("More than one user entry with the same email exists!");
    }
    if (matchingUsers.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(matchingUsers.get(0));
  }

  /**
   *
   * @param id
   * @return
   * @since
   */
  public Optional<User> findById(String id) {
    return dataStorage.findUserById(id);
  }

  /**
   * Adds a user to the repository.
   * @param user
   * @return
   * @since
   */
  public boolean addUser(User user) {
    Optional<User> userSearch = findById(user.getId());
    if (userSearch.isPresent()) {
      return false;
    }
    return dataStorage.storeUser(user);
  }

}
