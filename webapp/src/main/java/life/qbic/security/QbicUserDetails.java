package life.qbic.security;

import java.util.Collection;
import java.util.List;
import life.qbic.usermanagement.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * <b>Implementation of the UserDetails interface</b>
 *
 * Since we use our own implementation of the user class ({@link User}), we have
 * to tell Spring how to access certain user information.
 *
 * This implementation encapsulate the {@link User} class but integrates it in the security context,
 * so we can make use of it.
 * @since 1.0.0
 */
public class QbicUserDetails implements UserDetails {

  private User user;

  /**
   * Constructor to use and embed a {@link User} entity.
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
    return user.getEncryptedPassword();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
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
    return true;
  }
}
