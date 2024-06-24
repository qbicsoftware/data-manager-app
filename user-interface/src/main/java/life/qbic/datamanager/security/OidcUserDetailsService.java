package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Optional;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
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
  private final UserInformationService userInformationService;


  public OidcUserDetailsService(
      @Autowired UserAuthorityProvider userAuthorityProvider,
      @Autowired UserInformationService userInformationService) {
    this.userAuthorityProvider = requireNonNull(userAuthorityProvider,
        "userAuthorityProvider must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUserInfo userInfo = new OidcUserInfo(userRequest.getIdToken().getClaims());
    var defaultOidcUser = new DefaultOidcUser(new ArrayList<>(), userRequest.getIdToken());
    Optional<UserInfo> localUser = userInformationService.findByOidc(defaultOidcUser.getName(),
        defaultOidcUser.getIssuer().toString());

    if (localUser.isPresent()) {
      var user = localUser.get();
      var authorities = userAuthorityProvider.getAuthoritiesByUserId(
          user.id());
      return new QbicOidcUser(authorities, userRequest.getIdToken(), userInfo, user.id());
    }
    return defaultOidcUser;
  }
}
