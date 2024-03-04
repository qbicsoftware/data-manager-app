package life.qbic.projectmanagement.application.authorization.acl;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>ProjectPermission Service</b>
 * <p>
 * A service handling project-scoped permissions.
 */
public interface ProjectAccessService {

  enum ProjectRole {
    //the order matters from least powerful to most powerful
    READER("reader"),
    EDITOR("editor"),
    ADMIN("admin"),
    OWNER("owner");
    private final String label;

    ProjectRole(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }


  static ProjectRole getRole(Collection<Permission> permissions) {
    ProjectRole[] allRoles = ProjectRole.values();
    for (int i = allRoles.length - 1; i >= 0; i--) {
      ProjectRole projectRole = allRoles[i];
      if (permissions.containsAll(getPermissions(projectRole))) {
        return projectRole;
      }
    }
    return null;
  }

  static Collection<Permission> getPermissions(ProjectRole projectRole) {
    return switch (projectRole) {
      case READER -> List.of(BasePermission.READ);
      case EDITOR -> List.of(BasePermission.READ, BasePermission.WRITE);
      case ADMIN -> List.of(BasePermission.ADMINISTRATION);
      case OWNER -> List.of(BasePermission.READ, BasePermission.WRITE, BasePermission.CREATE,
          BasePermission.DELETE, BasePermission.ADMINISTRATION);
    };
  }


  record ProjectCollaborator(String userId, ProjectRole projectRole) {

  }

  void addProjectCollaborator(ProjectId projectId, String userId, ProjectRole projectRole);

  void addProjectRole(ProjectId projectId, String userId, ProjectRole projectRole);

  void replaceProjectRole(ProjectId projectId, String userId, ProjectRole oldRole,
      ProjectRole replacement);

  void removeProjectRole(ProjectId projectId, String userId, ProjectRole projectRole);

  void removeCollaborator(ProjectId projectId, String userId);

//  /**
//   * Lists all users which have a permission within the specific project
//   *
//   * @param projectId the identifier of the project
//   * @return a list of user ids which are associated with the project
//   */
//  List<String> listUserIds(ProjectId projectId);

  List<ProjectCollaborator> listCollaborators(ProjectId projectId);
//
//  /**
//   * Lists all active users which have a permission within the specific project
//   *
//   * @param projectId the identifier of the project
//   * @return a list of user ids of active users that are associated with the project
//   */
//  List<String> listActiveUserIds(ProjectId projectId);

  /**
   * Grant a specific permission on a project for a user
   *
   * @param userId   the id of the user for which to grant the permission
   * @param projectId  the project for which the permission shall be granted
   * @param permission the permission to grant
   */
  void grant(String userId, ProjectId projectId, Permission permission);

  @Transactional
  void grant(String userId, ProjectId projectId, List<Permission> permissions);

  /**
   * Grant a specific permission on a project for a user
   *
   * @param authority  the authorityfor which to grant the permission
   * @param projectId  the project for which the permission shall be granted
   * @param permission the permission to grant
   */
  void grantToAuthority(GrantedAuthority authority, ProjectId projectId, Permission permission);

  /**
   * Grant a specific permission on a project for a user
   *
   * @param authority   the authorityfor which to grant the permission
   * @param projectId   the project for which the permission shall be granted
   * @param permissions the permissions to grant
   */
  void grantToAuthority(GrantedAuthority authority, ProjectId projectId,
      List<Permission> permissions);

  /**
   * Deny a specific permission o a project for a user
   *
   * @param userId   the id of the user for which to deny the permission
   * @param projectId  the project for which the permission shall be denied
   * @param permission the permission to deny
   */
  void deny(String userId, ProjectId projectId, Permission permission);

  void deny(String userId, ProjectId projectId, List<Permission> permissions);

  /**
   * Deny all permissions to a project for a specific user. This effectively removes a user from a
   * project.
   *
   * @param userId  the id of the user losing the permissions
   * @param projectId the project for which to deny the permissions
   */
  void denyAll(String userId, ProjectId projectId);

  List<String> listAuthorities(ProjectId projectId);

}
