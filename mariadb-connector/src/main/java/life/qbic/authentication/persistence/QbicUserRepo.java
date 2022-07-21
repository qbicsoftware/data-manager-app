package life.qbic.authentication.persistence;


import java.util.List;
import life.qbic.identity.domain.user.EmailAddress;
import life.qbic.identity.domain.user.User;
import life.qbic.identity.domain.user.UserId;
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
public interface QbicUserRepo extends CrudRepository<User, UserId> {

  /**
   * Find users by email address in the persistent data storage
   *
   * @param emailAddress the email address to filter users for
   * @return a list of matching users that have the given email address
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
}
