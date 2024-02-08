package life.qbic.identity.domain.model.token;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface TokenRepository {

  void save(PersonalAccessToken token);

  void delete(PersonalAccessToken token);

}
