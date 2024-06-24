package life.qbic.identity.domain.repository;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import org.springframework.data.domain.Pageable;

/**
 * <b> Provides stateless access and storage functionality for {@link User} entities. </b>
 *
 * @since 1.0.0
 */
public class UserRepository implements Serializable {

  @Serial
  private static final long serialVersionUID = 5576670098610784078L;

  private static UserRepository instance;

  private final UserDataStorage dataStorage;

  protected UserRepository(UserDataStorage dataStorage) {
    this.dataStorage = dataStorage;
  }

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
    if (instance == null) {
      instance = new UserRepository(dataStorage);
    }
    return instance;
  }

  /**
   * Searches for a user with the provided mail address
   *
   * <p>Note: A runtime exception is thrown, when there is more than one user found. We want the
   * mail addresses to be unique in the user context, but they might change over time. So emails are
   * not suitable as entity identifiers but still need to be unique in the user management context
   * at any given time.
   *
   * <p>
   *
   * @param emailAddress the mail to find a matching user entry for
   * @return the user object wrapped in an {@link Optional} if found, otherwise returns
   * {@link Optional#empty()}
   * @throws RuntimeException if there is more than one user matching the mail address
   * @since 1.0.0
   */
  public Optional<User> findByEmail(EmailAddress emailAddress) throws RuntimeException {
    var matchingUsers = dataStorage.findUsersByEmailAddress(emailAddress);
    if (matchingUsers.size() > 1) {
      throw new RuntimeException("More than one user entry with the same mail address exists!");
    }
    if (matchingUsers.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingUsers.get(0));
    }
  }

  public Optional<User> findByUserName(String userName) {
    return dataStorage.findUserByUserName(userName);
  }

  /**
   * Searches for a user matching a provided userId
   *
   * @param userId the user's unique id, accessible via {@link User#id()}
   * @return the user if present in the repository, else returns an {@link Optional#empty()}.
   * @since 1.0.0
   */
  public Optional<User> findById(UserId userId) {
    return dataStorage.findUserById(userId);
  }

  /**
   * Retrieves all active users within the data manager application
   *
   * @return List of {@link User} objects with their active field set to true
   */
  public List<User> findAllActiveUsers() {
    return dataStorage.findAllActiveUsers();
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
    if (doesUserExistWithId(user.id()) || doesUserExistWithEmail(user.emailAddress())) {
      throw new UserStorageException();
    }
    saveUser(user);
  }

  public List<User> findByUserNameContainingIgnoreCaseAndActiveTrue(String userName,
      Pageable pageable) {
    return dataStorage.findByUserNameContainingIgnoreCaseAndActiveTrue(userName, pageable);
  }

  public Optional<User> findByOidc(String oidcId, String oidcIssuer) {
    return dataStorage.findByOidcIdEqualsAndOidcIssuerEquals(oidcId, oidcIssuer);
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
    if (!doesUserExistWithId(user.id())) {
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

  private boolean doesUserExistWithEmail(EmailAddress emailAddress) {
    return findByEmail(emailAddress).isPresent();
  }

  private boolean doesUserExistWithId(UserId id) {
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
