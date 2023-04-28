package life.qbic.authorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * <b>Project Role class</b>
 * <p>
 * Defines a role of a user in the context of a project.
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
