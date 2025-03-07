package life.qbic.projectmanagement.application.authorization.authorities;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.StringJoiner;
import org.springframework.security.core.GrantedAuthority;

/**
 * Represents a role of a user. A user role can provides a granted authority and can provide
 * additional authorities when assigned permissions.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "roles")
public class Role implements GrantedAuthority {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private long id;

  @Basic(fetch = FetchType.EAGER)
  @Column(name = "name")
  private String name;

  @Basic(fetch = FetchType.EAGER)
  @Column(name = "description")
  private String description;

  @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinTable(name = "role_permission",
      joinColumns = @JoinColumn(name = "userRoleId"),
      inverseJoinColumns = @JoinColumn(name = "permissionId"))
  private List<Permission> permissions = new ArrayList<>();


  protected Role() {
  }

  protected Role(long id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public String name() {
    requireNonNull(this.name);
    return name;
  }

  public Optional<String> description() {
    return Optional.ofNullable(description);
  }


  public List<Permission> permissions() {
    return permissions;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Role.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("name='" + name + "'")
        .add("description='" + description + "'")
        .add("permissions=" + permissions)
        .toString();
  }

  @Override
  public String getAuthority() {
    return "ROLE_" + name();
  }

  public long getId() {
    return id;
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
