package life.qbic.authorization;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;

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
