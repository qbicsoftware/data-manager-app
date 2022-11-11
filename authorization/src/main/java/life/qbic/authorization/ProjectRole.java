package life.qbic.authorization;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Entity
@IdClass(ProjectRoleId.class)
@Table(name = "users_project_roles")
public class ProjectRole implements Serializable {

  @Id
  @Column(name = "userId")
  private UserId userId;
  @Id
  @Column(name = "projectId")
  private ProjectId projectId;
  @Column(name = "userRoleId")
  private String userRoleId;

  public UserId userId() {
    return userId;
  }

  public ProjectId projectId() {
    return projectId;
  }

  public String userRoleId() {
    return userRoleId;
  }
}
