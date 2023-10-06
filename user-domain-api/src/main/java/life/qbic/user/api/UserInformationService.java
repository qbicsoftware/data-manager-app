package life.qbic.user.api;

import java.util.Optional;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface UserInformationService {

  Optional<UserInfo> findByEmail(String emailAddress);

  Optional<UserInfo> findById(String userId);

}
