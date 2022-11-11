package life.qbic.authorization;

import java.util.List;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public interface ProjectPermissionService {

  /**
   * ToDo
   *
   * @param userId
   * @param projectId
   * @return a list of {@link SimpleGrantedAuthority} for the provided project
   */
  List<SimpleGrantedAuthority> loadUserPermissions(UserId userId, ProjectId projectId);
}
