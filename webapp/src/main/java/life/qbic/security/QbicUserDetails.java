package life.qbic.security;

import java.util.Collection;
import java.util.List;
import life.qbic.usermanagement.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * <b>QBiC User Details</b>
 *
 * <p>Implements springs {@link UserDetails} class for the QBiC context</p>
 *
 * @since 1.0.0
 */
public class QbicUserDetails implements UserDetails {

  private final User user;

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
