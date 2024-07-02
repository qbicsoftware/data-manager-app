package life.qbic.identity.infrastructure;


import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

/**
 * <b>QBiC user repository interface</b>
 *
 * <p>This interface will be automatically detected by Spring on application startup and create an
 * instance of this class automatically.
 *
 * <p>Since it extends the {@link CrudRepository} class from Spring, no need to write queries. The
 * framework will do that for us.
 *
 * @since 1.0.0
 */
public interface QbicUserRepo extends JpaRepository<User, UserId> {

  /**
   * Find users by mail address in the persistent data storage
   *
   * @param emailAddress the mail address to filter users for
   * @return a list of matching users that have the given mail address
   * @since 1.0.0
   */
  List<User> findUsersByEmailAddress(EmailAddress emailAddress);

  /**
   * Find a user entity by its user id.
   *
   * @param id the user id
   * @return the user matching the id, is <code>null</code> if no matching user was found.
   * @since 1.0.0
   */
  User findUserById(UserId id);

  /**
   * Gets all active Users in the data manager application
   *
   * @return a list of matching users which are set to active in the data manager application
   */
  List<User> findUsersByActiveTrue();

  User findUserByUserName(String userName);

  List<User> findAllByUserNameContainingIgnoreCaseAndActiveTrue(String username, Pageable pageable);

  Optional<User> findByOidcIdEqualsAndOidcIssuerEquals(String oidcId, String oidcIssuer);
}
