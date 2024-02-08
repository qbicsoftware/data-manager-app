package life.qbic.identity.api;

import java.time.Duration;
import java.util.Collection;

/**
 * <b>Personal Access PersonalAccessToken Service</b>
 *
 * <p>An identity domain service interface that enables the creation and query of
 * personal access token for users.</p>
 *
 * @since 1.0.0
 */
public interface PersonalAccessTokenService {

  /**
   * Creates a personal access token for a user with the given id.
   *
   * @param userId the user id of the user the token shall be created for
   * @param duration the duration until the token has expired
   * @return the raw token after successful creation
   * @since 1.0.0
   */
  RawToken create(String userId, String description, Duration duration) throws UnknownUserIdException;

  /**
   * Lists all personal access token entries for a user with a given id.
   *
   * @param userId the user's id the service shall search for existing tokens
   * @return a collection of personal access token entries associated with a user id
   * @since 1.0.0
   */
  Collection<PersonalAccessToken> find(String userId);

}
