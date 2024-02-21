package life.qbic.identity.domain.model.token;

import java.util.Collection;
import java.util.Optional;

/**
 * Token repository interface
 * <p>
 * Provides access to the persistence layer that handles the {@link PersonalAccessToken} storage,
 * access and deletion.
 *
 * @since 1.0.0
 */
public interface TokenRepository {

  Collection<PersonalAccessToken> findAllByUserId(String userId);

  void save(PersonalAccessToken token);

  void delete(PersonalAccessToken token);

  Optional<PersonalAccessToken> find(String accessTokenId);

}
