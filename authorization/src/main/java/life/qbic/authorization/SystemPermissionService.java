package life.qbic.authorization;

import java.util.List;
import life.qbic.authentication.domain.user.concept.UserId;
import org.springframework.security.core.GrantedAuthority;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface SystemPermissionService {

  /**
   * @param userId
   * @return a list of {@link GrantedAuthority} for the provided project
   */
  List<GrantedAuthority> loadUserPermissions(UserId userId);

}
