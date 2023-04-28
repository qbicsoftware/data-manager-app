package life.qbic.authorization;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Represents a role of a user. A user role can provides a granted authority and can provide
 * additional authorities when assigned permissions.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "user_roles")
public class UserRole implements GrantedAuthority {

  @Id
  @Column(name = "id")
  private String id;

  @Basic(fetch = FetchType.EAGER)
  @Column(name = "roleDescription")
  private String description;

  @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinTable(name = "user_roles_permissions",
      joinColumns = @JoinColumn(name = "userRoleId"),
      inverseJoinColumns = @JoinColumn(name = "permissionId"))
  private List<Permission> permissions = new ArrayList<>();


  protected UserRole() {
  }

  protected UserRole(String id, String description) {
    this.id = id;
    this.description = description;
  }

  public static UserRole with(String id, String description) {
    return new UserRole(id, description);
  }

  public String id() {
    requireNonNull(id);
    return id;
  }

  public Optional<String> description() {
    return Optional.ofNullable(description);
  }


  public List<Permission> permissions() {
    return permissions;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", UserRole.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("description='" + description + "'")
        .add("permissions=" + permissions)
        .toString();
  }

  @Override
  public String getAuthority() {
    return "ROLE_" + id();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else {
      return obj instanceof GrantedAuthority && this.getAuthority()
          .equals(((GrantedAuthority) obj).getAuthority());
    }
  }

  @Override
  public int hashCode() {
    return getAuthority().hashCode();
  }
}
