package life.qbic.authorization;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

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
  private String userId;
  @Id
  @Column(name = "projectId")
  private String projectId;
  @Column(name = "userRoleId")
  private String userRoleId;

  public String userId() {
    return userId;
  }

  public String projectId() {
    return projectId;
  }

  public String userRoleId() {
    return userRoleId;
  }
}
