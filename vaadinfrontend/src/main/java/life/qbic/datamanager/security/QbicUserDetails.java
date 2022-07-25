package life.qbic.datamanager.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import life.qbic.authentication.domain.user.concept.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
  private final transient User user;

  /**
   * Constructor to use and embed a {@link User} entity.
   *
   * @param user the user to embed
   * @since 1.0.0
   */
  public QbicUserDetails(User user) {
    this.user = user;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("USER"));
  }

  @Override
  public String getPassword() {
    return user.getEncryptedPassword().get();
  }

  @Override
  public String getUsername() {
    return user.emailAddress().get();
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
    return user.isActive();
  }
}
