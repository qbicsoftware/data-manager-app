package life.qbic.identity.domain.model.token;

import java.util.Collection;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface TokenRepository {

  Collection<PersonalAccessToken> findAllByUserId(String userId);

  void save(PersonalAccessToken token);

  void delete(PersonalAccessToken token);

}
