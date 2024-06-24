package life.qbic.identity.api;

import java.util.Optional;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface UserPasswordService {

  public Optional<UserPassword> findForUser(String userId);

}
