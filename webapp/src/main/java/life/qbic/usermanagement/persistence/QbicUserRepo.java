package life.qbic.usermanagement.persistence;

import java.util.List;
import life.qbic.domain.user.EmailAddress;
import life.qbic.domain.user.User;
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
public interface QbicUserRepo extends CrudRepository<User, String> {

  /**
   * Find users by emailAddress value in the persistent data storage
   *
   * @param emailAddress the emailAddress value to filter users for
   * @return a list of matching users that have the given emailAddress value
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
  User findUserById(String id);
}
