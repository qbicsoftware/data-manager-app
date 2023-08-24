package life.qbic.authorization.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * <b>Implementation of the UserDetails interface</b>
 *
 * <p>Since we use our own implementation of the user class ({@link User}), we have to tell Spring
 * how to access certain user information.
 *
 * <p>This implementation encapsulate the {@link User} class but integrates it in the security
 * context, so we can make use of it.
 *
 * @since 1.0.0
 */
public class QbicUserDetails implements UserDetails {

  @Serial
  private static final long serialVersionUID = 5812210012669790933L;
  private final UserId userId;
  private final String username;
  private final String password;

  private final boolean active;

  private final List<GrantedAuthority> grantedAuthorities;

  /**
   * Constructor to use and embed a {@link User} entity.
   *
   * @param user the user to embed
   * @since 1.0.0
   */
  public QbicUserDetails(User user, List<GrantedAuthority> grantedAuthorities) {
    this.userId = user.id();
    this.username = user.emailAddress().get();
    this.password = user.getEncryptedPassword().get();
    this.active = user.isActive();
    this.grantedAuthorities = List.copyOf(grantedAuthorities);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.copyOf(grantedAuthorities);
  }

  public UserId getUserId() {
    return userId;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }

  public boolean hasAuthority(GrantedAuthority authority) {
    return getAuthorities().stream()
        .anyMatch(it -> it.getAuthority().equals(authority.getAuthority()));
  }
}
