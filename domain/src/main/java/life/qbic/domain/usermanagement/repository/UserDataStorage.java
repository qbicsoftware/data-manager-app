package life.qbic.domain.usermanagement.repository;

import life.qbic.domain.usermanagement.User;

import java.util.List;
import java.util.Optional;

/**
 * <b>User Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link User} data storage.
 *
 * @since 1.0.0
 */
public interface UserDataStorage {

  /**
   * Searches for any available user entities with the provided email address.
   *
   * <p>Note, that the implementation must not make any assumptions by number of occurrences, even
   * for the email address. The implementation shall return any user entry with the provided email
   * address and leave the logic to the application layer.
   *
   * @param email the email address to use as search filter
   * @return a list of matching {@link User} entries. Is empty, if no matching user is present with
   *     the provided email address
   * @since 1.0.0
   */
  List<User> findUsersByEmail(String email);

  /**
   * Saves a {@link User} entity permanently.
   *
   * @param user the user to store
   * @since 1.0.0
   */
  void save(User user);

  /**
   * Find a user entity based on its user id.
   *
   * @param id the user id to search for a matching entry in the storage
   * @return the user object, or {@link Optional#empty()} if no entity with the provided id was
   *     found.
   * @since 1.0.0
   */
  Optional<User> findUserById(String id);
}
