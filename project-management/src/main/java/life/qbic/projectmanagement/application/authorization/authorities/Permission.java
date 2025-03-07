package life.qbic.projectmanagement.application.authorization.authorities;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import static java.util.Objects.requireNonNull;
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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private long id;

  @Basic(fetch = FetchType.EAGER)
  @Column(name = "name")
  private String name;

  @Basic(fetch = FetchType.EAGER)
  @Column(name = "description")
  private String description;

  protected Permission() {
  }

  protected Permission(long id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public String name() {
    requireNonNull(name);
    return name;
  }

  public Optional<String> description() {
    return Optional.ofNullable(description);
  }

  @Transient
  @Override
  public String getAuthority() {
    return name();
  }

  @Override
  public String toString() {
    return name();
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
