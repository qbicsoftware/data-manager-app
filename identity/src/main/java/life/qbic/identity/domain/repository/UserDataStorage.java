package life.qbic.identity.domain.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import org.springframework.data.domain.Pageable;

/**
 * <b>User Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link User} data storage.
 *
 * @since 1.0.0
 */
public interface UserDataStorage {

  /**
   * Searches for any available user entities with the provided mail address.
   *
   * <p>Note, that the implementation must not make any assumptions by number of occurrences, even
   * for the mail address. The implementation shall return any user entry with the provided mail
   * address and leave the logic to the application layer.
   *
   * @param emailAddress the mail address to use as search filter
   * @return a list of matching {@link User} entries. Is empty, if no matching user is present with
   * the provided mail address
   * @since 1.0.0
   */
  List<User> findUsersByEmailAddress(EmailAddress emailAddress);

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
   * found.
   * @since 1.0.0
   */
  Optional<User> findUserById(UserId id);

  /**
   * Searches for all user entities which are set to active.
   *
   * @return a list of matching {@link User} entries. Is empty, if the user did not active its
   * account
   */
  List<User> findAllActiveUsers();

  Optional<User> findUserByUserName(String userName);

  List<User> queryActiveUsersWithFilter(String filter, Pageable pageable);

  Optional<User> findByOidcIdEqualsAndOidcIssuerEquals(String oidcId, String oidcIssuer);
}
