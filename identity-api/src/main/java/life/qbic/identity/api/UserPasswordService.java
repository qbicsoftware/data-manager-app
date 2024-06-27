package life.qbic.identity.api;

import java.util.Optional;

/**
 * A service for retrieving encrypted user passwords.
 *
 * @since 1.2.0
 */
public interface UserPasswordService {

  /**
   * @param userId the identifier of the user in the datamanager user management
   * @return the {@link UserPassword} for a user with the given identifier
   */
  Optional<UserPassword> findForUser(String userId);

}
