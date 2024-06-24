package life.qbic.datamanager.security;

import java.util.ArrayList;
import java.util.List;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Component
public class OidcUserDetailsService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private final UserAuthorityProvider userAuthorityProvider;

  public OidcUserDetailsService(
      @Autowired UserAuthorityProvider userAuthorityProvider) {
    this.userAuthorityProvider = userAuthorityProvider;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUserInfo userInfo = new OidcUserInfo(userRequest.getIdToken().getClaims());
    QbicOidcUser qbicOidcUser = new QbicOidcUser(new ArrayList<>(), userRequest.getIdToken(),
        userInfo);
    List<GrantedAuthority> authorities = userAuthorityProvider.getAuthoritiesByUserId(
        qbicOidcUser.getName());
    return new QbicOidcUser(authorities, userRequest.getIdToken(), userInfo);
  }
}
