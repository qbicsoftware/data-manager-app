package life.qbic.identity.api;

import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface AuthenticationToUserIdTranslator {

  /**
   * Tranlsates an authentication object to the associated userId
   *
   * @param authentication
   * @return
   */
  Optional<String> translateToUserId(Authentication authentication);

}
