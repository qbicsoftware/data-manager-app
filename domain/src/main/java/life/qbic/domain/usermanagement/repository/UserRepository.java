package life.qbic.domain.usermanagement.repository;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import life.qbic.domain.usermanagement.User;

/**
 * <b> Provides stateless access and storage functionality for {@link User} entities. </b>
 *
 * @since 1.0.0
 */
public class UserRepository implements Serializable {

  @Serial
  private static final long serialVersionUID = 5576670098610784078L;

  private static UserRepository INSTANCE;

  private final UserDataStorage dataStorage;

  /**
   * Retrieves a Singleton instance of a user {@link UserRepository}. In case this method is called
   * the first time, a new instance is created.
   *
   * @param dataStorage an implementation of {@link UserDataStorage}, handling the low level
   *                    persistence layer access.
   * @return a Singleton instance of a user repository.
   * @since 1.0.0
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
   *
   * <p>Note: A runtime exception is thrown, when there is more than one user found. We want the
   * email addresses to be unique in the user context, but they might change over time. So emails
   * are not suitable as entity identifiers but still need to be unique in the user management
   * context at any given time.
   *
   * <p>
   *
   * @param email the email to find a matching user entry for
   * @return the user object wrapped in an {@link Optional} if found, otherwise returns
   * {@link Optional#empty()}
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
    } else {
      return Optional.of(matchingUsers.get(0));
    }
  }

  /**
   * Searches for a user matching a provided userId
   *
   * @param userId the user's unique id, accessible via {@link User#getId()}
   * @return the user if present in the repository, else returns an {@link Optional#empty()}.
   * @since 1.0.0
   */
  public Optional<User> findById(String userId) {
    return dataStorage.findUserById(userId);
  }

  /**
   * Adds a user to the repository. Publishes all domain events of the user if successful. If
   * unsuccessful, throws a {@link UserStorageException} Exception.
   *
   * @param user the user that shall be added to the repository
   * @throws UserStorageException if the user could not be added to the repository
   * @since 1.0.0
   */
  public void addUser(User user) throws UserStorageException {
    if (doesUserExistWithId(user.getId()) || doesUserExistWithEmail(user.getEmail())) {
      throw new UserStorageException();
    }
    saveUser(user);
  }

  /**
   * Updates a user in the repository. Publishes all domain events of the user if successful. If
   * unsuccessful, throws a {@link UserStorageException}
   *
   * @param user the updated user state to write to the repository
   * @throws UserStorageException if the user could not be updated in the repository
   * @since 1.0.0
   */
  public void updateUser(User user) throws UserStorageException {
    if (!doesUserExistWithId(user.getId())) {
      throw new UserStorageException();
    }
    saveUser(user);
  }

  private void saveUser(User user) {
    try {
      dataStorage.save(user);
    } catch (Exception e) {
      throw new UserStorageException(e);
    }
  }

  private boolean doesUserExistWithEmail(String email) {
    return findByEmail(email).isPresent();
  }

  private boolean doesUserExistWithId(String id) {
    return findById(id).isPresent();
  }

  public static class UserStorageException extends RuntimeException {


    public UserStorageException() {
    }

    public UserStorageException(Throwable cause) {
      super(cause);
    }
  }
}
