package life.qbic.projectmanagement.application.authorization;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class QbicOidcUser extends DefaultOidcUser {

  private final String qbicUserId;
  private final boolean active;

  public QbicOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
      OidcUserInfo userInfo, String qbicUserId, boolean active) {
    super(authorities, idToken, userInfo);
    this.qbicUserId = qbicUserId;
    this.active = active;
  }

  public String getQbicUserId() {
    return qbicUserId;
  }

  public boolean isActive() {
    return active;
  }
}
