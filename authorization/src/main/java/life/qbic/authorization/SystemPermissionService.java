package life.qbic.authorization;

import life.qbic.authentication.domain.user.concept.UserId;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * <b>System Permission Service</b>
 * <p>
 * A service handling system-scoped permissions.
 */
public interface SystemPermissionService {

  /**
   * @param userId the user to load the permissions for
   * @return a list of {@link GrantedAuthority} for the provided project
   */
  List<GrantedAuthority> loadUserPermissions(UserId userId);

}
