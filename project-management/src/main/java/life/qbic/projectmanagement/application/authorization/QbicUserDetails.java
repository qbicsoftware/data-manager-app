package life.qbic.projectmanagement.application.authorization;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
public class QbicUserDetails implements UserDetails, Serializable {
  @Serial
  private static final long serialVersionUID = 5812210012669790933L;
  private final User user;
  private final List<GrantedAuthority> grantedAuthorities;

  /**
   * Constructor to use and embed a {@link User} entity.
   *
   * @param user the user to embed
   * @since 1.0.0
   */
  public QbicUserDetails(User user, List<GrantedAuthority> grantedAuthorities) {
    this.user = Objects.requireNonNull(user);
    this.grantedAuthorities = List.copyOf(grantedAuthorities);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.copyOf(grantedAuthorities);
  }

  public String getUserId() {
    return user.id();
  }

  @Override
  public String getPassword() {
    return user.encryptedPassword();
  }

  public String getEmailAddress() { return user.emailAddress(); }
  @Override
  public String getUsername() {
    // This is what is returned by the authentication module and used by ACL classes
    // We want the id instead of email address
    return user.id();
  }

  /**
   * Returns the unique user display name displayed to the user. Has to be distinguished from the
   * Spring default method getUserName() which returns the userId to which the ACL permissions are
   * linked
   *
   * @return the unique user display name defined to the user
   */
  public String platformUserName() {
    return user.platformUserName();
  }

  public String fullName() {
    return user.fullName();
  }

  public String oidc() {
    return user.oidc();
  }

  public String oidcIssuer() {
    return user.oidcIssuer();
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

  public boolean hasAuthority(GrantedAuthority authority) {
    return hasAuthority(authority.getAuthority());
  }

  public boolean hasAuthority(String authority) {
    return getAuthorities().stream()
        .anyMatch(it -> it.getAuthority().equals(authority));
  }
}
