package life.qbic.authorization;

import java.util.List;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.security.core.GrantedAuthority;

/**
 * <b>ProjectPermission Service</b>
 * <p>
 * A service handling project-scoped permissions.
 */
public interface ProjectPermissionService {

  /**
   * Lists the permissions granted to a specific user for a specific project
   *
   * @param userId    the identifier of the user
   * @param projectId the identifier of the project
   * @return a list of authorities granted to the user for the project
   */
  List<? extends GrantedAuthority> loadUserPermissions(UserId userId, ProjectId projectId);
}
