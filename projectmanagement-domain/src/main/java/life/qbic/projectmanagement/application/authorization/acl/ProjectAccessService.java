package life.qbic.projectmanagement.application.authorization.acl;

import java.util.List;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

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
   * @return a list of user ids which are associated with the project
   */
  List<String> listUsers(ProjectId projectId);

  /**
   * Lists all active users which have a permission within the specific project
   *
   * @param projectId the identifier of the project
   * @return a list of user ids which are associated with the project
   */
  List<String> listActiveUsers(ProjectId projectId);

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

  @Transactional
  void grant(String username, ProjectId projectId, List<Permission> permissions);

  /**
   * Grant a specific permission on a project for a user
   *
   * @param authority  the authorityfor which to grant the permission
   * @param projectId  the project for which the permission shall be granted
   * @param permission the permission to grant
   */
  void grantToAuthority(GrantedAuthority authority, ProjectId projectId, Permission permission);

  @Transactional
  void grantToAuthority(GrantedAuthority authority, ProjectId projectId,
      List<Permission> permissions);

  /**
   * Deny a specific permission o a project for a user
   *
   * @param username   the username of the user for which to deny the permission
   * @param projectId  the project for which the permission shall be denied
   * @param permission the permission to deny
   */
  void deny(String username, ProjectId projectId, Permission permission);

  @Transactional
  void deny(String username, ProjectId projectId, List<Permission> permissions);

  /**
   * Deny all permissions to a project for a specific user. This effectively removes a user from a
   * project.
   *
   * @param username  the user losing the permissions
   * @param projectId the project for which to deny the permissions
   */
  void denyAll(String username, ProjectId projectId);

  List<String> listAuthorities(ProjectId projectId);

  List<String> listAuthoritiesForPermission(ProjectId projectId, Permission permission);
}
