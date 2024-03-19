package life.qbic.projectmanagement.application.authorization.acl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;

/**
 * <b>ProjectPermission Service</b>
 * <p>
 * A service handling project-scoped permissions.
 */
public interface ProjectAccessService {

  enum ProjectRole {
    //the order matters from least powerful to most powerful
    READ("read",
        "Can read project information. Can also download data associated with the project."),
    WRITE("write",
        "Can read and edit project information. Can also download data associated with the project."),
    ADMIN("admin", "Can read, edit, download project information. Can also manage project access."),
    OWNER("owner", "Has complete access to this project."),
    ;
    private final String label;
    private final String description;

    ProjectRole(String label, String description) {
      this.description = description;
      this.label = label;
    }

    public String label() {
      return label;
    }

    public String description() {
      return description;
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

    public Collection<Permission> toPermissions() {
      return toPermissions(this);
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
        case READ ->
            "Recommended for people who want to view a project and download associated data.";
        case WRITE -> "Recommended for people who edit the project.";
        case ADMIN ->
            "Recommended for people who need full access to the project, including managing project access.";
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
   * Adds a collaborator to the given project
   *
   * @param projectId
   * @param userId
   * @param projectRole
   */
  void addCollaborator(ProjectId projectId, String userId, ProjectRole projectRole);

  void removeCollaborator(ProjectId projectId, String userId);

  void changeRole(ProjectId projectId, String userId, ProjectRole projectRole);

  void addAuthorityAccess(ProjectId projectId, String authority, ProjectRole projectRole);

  void removeAuthorityAccess(ProjectId projectId, String authority);

  void changeAuthorityAccess(ProjectId projectId, String authority, ProjectRole projectRole);

  List<ProjectCollaborator> listCollaborators(ProjectId projectId);

  void removeProject(ProjectId projectId);
}
