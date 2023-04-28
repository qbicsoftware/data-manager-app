package life.qbic.authorization;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Represents a permission to do something
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "permissions")
public class Permission implements GrantedAuthority {

  @Id
  @Column(name = "id")
  private String id;

  @Basic(fetch = FetchType.EAGER)
  @Column(name = "permissionDescription")
  private String description;

  public static Permission with(String id, String description) {
    return new Permission(id, description);
  }

  protected Permission() {

  }

  protected Permission(String id, String description) {
    this.id = id;
    this.description = description;
  }

  public String id() {
    requireNonNull(id);
    return id;
  }

  public Optional<String> description() {
    return Optional.ofNullable(description);
  }

  @Transient
  @Override
  public String getAuthority() {
    return id();
  }

  @Override
  public String toString() {
    return id();
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
