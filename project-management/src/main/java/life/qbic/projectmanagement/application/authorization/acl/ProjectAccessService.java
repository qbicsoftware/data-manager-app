package life.qbic.projectmanagement.application.authorization.acl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.GrantedAuthority;

/**
 * <b>ProjectPermission Service</b>
 * <p>
 * A service handling project-scoped permissions.
 */
public interface ProjectAccessService {

  /**
   * Initializes an ACL for a given project with the user as owner.
   *
   * @param projectId the project id
   * @param userId the owner
   */
  void initializeProject(ProjectId projectId, String userId);

  void addCollaborator(ProjectId projectId, String userId, ProjectRole projectRole);

  void removeCollaborator(ProjectId projectId, String userId);

  void changeRole(ProjectId projectId, String userId, ProjectRole projectRole);

  void addAuthorityAccess(ProjectId projectId, String authority, ProjectRole projectRole);

  void removeAuthorityAccess(ProjectId projectId, String authority);

  void changeAuthorityAccess(ProjectId projectId, String authority, ProjectRole projectRole);

  /**
   * Retrieves the projects to which the provided sid has access to
   *
   * @param sid String representation of the {@link Sid}
   *            usually contained within the principal of a user as a role within the {@link GrantedAuthority}
   *            or the Id of the user itself
   * @return List of {@link ProjectId} which are accessible to the provided Sid
   */
  List<ProjectId> getAccessibleProjectsForSid(String sid);

  List<ProjectCollaborator> listCollaborators(ProjectId projectId);

  /**
   * Lists the principal (personal) users holding OWNER or ADMIN role on a project.
   *
   * <p>This is the non-authorization-annotated seam for background (JobRunr) consumers that
   * have no security context — e.g. the user-groups notification directive informing project
   * owners/admins about a membership change on a shared group (FEAT-USER-GROUPS-02 AC2). It
   * deliberately carries <b>no</b> {@code @PreAuthorize} and must only be invoked by trusted
   * callers.</p>
   *
   * <p>Group SIDs ({@code GROUP_} prefix) and system-role authorities are never returned; the
   * result only contains principal sids with a resolvable OWNER or ADMIN project role.</p>
   *
   * @param projectId the project to resolve administrators for
   * @return the principal user ids holding OWNER or ADMIN on the project (may be empty)
   * @since 1.22.0
   */
  List<String> listProjectAdministrators(ProjectId projectId);

  /**
   * Lists the user groups shared onto a project.
   *
   * <p>Groups ride as {@link GrantedAuthoritySid}s with the reserved {@code GROUP_} prefix
   * (strategy §4.3). Only the group identity, display name, description and granted project role
   * are exposed — never the member roster (visibility policy, strategy §5.1).</p>
   *
   * @param projectId the project id
   * @return the shared groups with their granted project role
   */
  List<SharedProjectGroup> listSharedGroups(ProjectId projectId);

  void removeProject(ProjectId projectId);

  enum ProjectRole {
    //the order matters from least powerful to most powerful
    READ("read",
        "View project and download data."),
    WRITE("write",
        "View and edit the project "),
    ADMIN("admin", "Full project access including project management"),
    OWNER("owner", "Has complete access to this project."),
    ;
    private final String label;
    private final String description;

    ProjectRole(String label, String description) {
      this.description = description;
      this.label = label;
    }

    static Optional<ProjectRole> fromPermissions(Collection<Permission> permissions) {
      ProjectRole[] projectRoles = ProjectRole.values();
      for (int roleIdx = projectRoles.length - 1; roleIdx >= 0; roleIdx--) {
        ProjectRole role = projectRoles[roleIdx];
        if (permissions.containsAll(toPermissions(role))) {
          return Optional.of(role);
        }
      }
      return Optional.empty();
    }

    private static Collection<Permission> toPermissions(ProjectRole projectRole) {
      return switch (projectRole) {
        case READ -> List.of(BasePermission.READ);
        case WRITE -> roleExtendedWith(ProjectRole.READ, BasePermission.WRITE);
        case ADMIN -> roleExtendedWith(ProjectRole.WRITE, BasePermission.ADMINISTRATION);
        case OWNER -> List.of(BasePermission.READ, BasePermission.WRITE, BasePermission.CREATE,
            BasePermission.DELETE, BasePermission.ADMINISTRATION);
      };
    }

    static Collection<Permission> roleExtendedWith(ProjectRole projectRole,
        Permission... permissions) {
      return Stream.concat(toPermissions(projectRole).stream(), Stream.of(permissions)).collect(
          Collectors.toUnmodifiableSet());
    }

    public String label() {
      return label;
    }

    public String description() {
      return description;
    }

    public Collection<Permission> toPermissions() {
      return toPermissions(this);
    }
  }

  /**
   * Renders the recommended use of a project role.
   * <p>
   * This is not in the ProjectRole as it might change for different reasons than the project roles
   * themselves.
   */
  class ProjectRoleRecommendationRenderer {

    private ProjectRoleRecommendationRenderer() {
      //static class - no constructor
    }

    public static String render(ProjectRole role) {
      return switch (role) {
        case READ -> "View all project data and metadata.";
        case WRITE -> "View and edit all project data and metadata";
        case ADMIN -> "View and edit all project data and metadata, manage access";
        case OWNER -> "Do not assign this role. This person owns the project.";
      };
    }
  }

  /**
   * A collaborator in a specific project.
   * @param userId the collaborating user
   * @param projectId the project on which the collaboration happens
   * @param projectRole the role of the user within the project
   */
  record ProjectCollaborator(String userId, ProjectId projectId, ProjectRole projectRole) {

  }

  /**
   * A user group shared onto a specific project.
   *
   * @param groupId the stable user group id
   * @param groupName the group's display name
   * @param groupDescription the group's description, may be {@code null}
   * @param projectId the project on which the group is shared
   * @param projectRole the project role granted to the group
   */
  record SharedProjectGroup(String groupId, String groupName, String groupDescription,
      ProjectId projectId, ProjectRole projectRole) {

  }
}
