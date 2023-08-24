package life.qbic.authorization.acl;

import java.util.List;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.GrantedAuthority;

/**
 * <b>ProjectPermission Service</b>
 * <p>
 * A service handling project-scoped permissions.
 */
public interface ProjectAccessService {

  /**
   * Lists all users which have a permission within the specific project
   *
   * @param projectId the identifier of the project
   * @return a list of UserIds which are associated with the project
   */
  List<UserId> listUsers(ProjectId projectId);

  /**
   * Lists all users which have a permission on the project
   *
   * @param projectId the identifier of the project
   * @return a list of usernames for which permissions exist for a project
   */
  List<String> listUsernames(ProjectId projectId);

  /**
   * Grant a specific permission on a project for a user
   *
   * @param username   the username of the user for which to grant the permission
   * @param projectId  the project for which the permission shall be granted
   * @param permission the permission to grant
   */
  void grant(String username, ProjectId projectId, Permission permission);

  /**
   * Grant a specific permission on a project for a user
   *
   * @param authority  the authorityfor which to grant the permission
   * @param projectId  the project for which the permission shall be granted
   * @param permission the permission to grant
   */
  void grantToAuthority(GrantedAuthority authority, ProjectId projectId, Permission permission);

  /**
   * Deny a specific permission o a project for a user
   *
   * @param username   the username of the user for which to deny the permission
   * @param projectId  the project for which the permission shall be denied
   * @param permission the permission to deny
   */
  void deny(String username, ProjectId projectId, Permission permission);

  /**
   * Deny all permissions to a project for a specific user. This effectively removes a user from a
   * project.
   *
   * @param username  the user losing the permissions
   * @param projectId the project for which to deny the permissions
   */
  void denyAll(String username, ProjectId projectId);

}
