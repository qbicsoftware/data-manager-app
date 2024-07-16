package life.qbic.identity.api;

import java.util.Optional;
import org.springframework.security.core.Authentication;

/**
 * Translates an {@link Authentication} object into an identifier of QBiC
 *
 * @since 1.2.0
 */
public interface AuthenticationToUserIdTranslator {

  /**
   * Translates an authentication object to the associated userId
   *
   * @param authentication
   * @return
   */
  Optional<String> translateToUserId(Authentication authentication);

}
