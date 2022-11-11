package life.qbic.authorization;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
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
}
