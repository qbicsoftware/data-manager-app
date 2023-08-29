package life.qbic.authorization.authorities;

import java.util.List;
import life.qbic.authentication.domain.user.concept.UserId;
import org.springframework.security.core.GrantedAuthority;

/**
 * Provides {@link GrantedAuthority} for a given user
 */
public interface UserAuthorityProvider {

  /**
   * List all authorities granted to the user with a matching userid
   *
   * @param userId the userid to retrieve granted authorities for
   * @return a list of authorities granted to the user
   */
  List<GrantedAuthority> getAuthoritiesByUserId(UserId userId);
}
