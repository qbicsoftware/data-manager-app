package life.qbic.authorization.authorities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * <b>System Role class</b>
 * <p>
 * Defines a role of a user in the context of the system
 */
@Entity
@Table(name = "user_role")
public class UserRole implements Serializable {


  /**
   * The database id of the link. Please do not touch.
   */
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "userId")
  private String userId;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "roleId")
  private Role userRole;

  public String userId() {
    return userId;
  }

  public Role role() {
    return userRole;
  }
}
