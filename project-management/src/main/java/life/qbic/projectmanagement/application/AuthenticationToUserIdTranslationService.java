package life.qbic.projectmanagement.application;

import java.util.Optional;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service(value = "userIdTranslator")
public class AuthenticationToUserIdTranslationService implements AuthenticationToUserIdTranslator {

  @Override
  public Optional<String> translateToUserId(Authentication authentication) {
    var principal = authentication.getPrincipal();
    if (principal instanceof QbicUserDetails qbicUserDetails) {
      return Optional.of(qbicUserDetails.getUserId());
    }
    if (principal instanceof QbicOidcUser qbicOidcUser) {
      return Optional.of(qbicOidcUser.getQbicUserId());
    }
    return Optional.empty();
  }
}
