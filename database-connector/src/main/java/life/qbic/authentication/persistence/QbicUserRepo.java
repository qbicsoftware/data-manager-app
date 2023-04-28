package life.qbic.authentication.persistence;


import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

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
}
