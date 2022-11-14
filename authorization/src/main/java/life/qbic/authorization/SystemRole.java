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
@Table(name = "users_system_roles")
public class SystemRole implements Serializable {


  /**
   * The database id of the link. Please do not touch.
   */
  @Id
  @Column(name = "id")
  private int id;

  @Column(name = "userId")
  private String userId;

  @Column(name = "userRoleId")
  private String userRoleId;

  public String userId() {
    return userId;
  }

  public String userRoleId() {
    return userRoleId;
  }
}
