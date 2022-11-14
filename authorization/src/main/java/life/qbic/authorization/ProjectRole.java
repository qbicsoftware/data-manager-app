package life.qbic.authorization;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Entity
@Table(name = "users_project_roles")
public class ProjectRole implements Serializable {

  /**
   * The database id of the link. Please do not touch.
   */
  @Id
  @Column(name = "id")
  private int id;

  @Column(name = "userId")
  private String userId;
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
