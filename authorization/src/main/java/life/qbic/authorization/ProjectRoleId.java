package life.qbic.authorization;

import java.io.Serializable;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ProjectRoleId implements Serializable {

  private UserId userId;

  private ProjectId projectId;

  protected ProjectRoleId() {

  }

  protected ProjectRoleId(UserId userId, ProjectId projectId) {
    this.userId = userId;
    this.projectId = projectId;
  }

  public static ProjectRoleId of(UserId userId, ProjectId projectId) {
    return new ProjectRoleId(userId, projectId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProjectRoleId that = (ProjectRoleId) o;

    if (!userId.equals(that.userId)) {
      return false;
    }
    return projectId.equals(that.projectId);
  }

  @Override
  public int hashCode() {
    int result = userId.hashCode();
    result = 31 * result + projectId.hashCode();
    return result;
  }
}
