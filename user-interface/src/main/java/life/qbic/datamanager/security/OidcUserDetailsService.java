package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
public class OidcUserDetailsService extends OidcUserService {

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
    OidcUser defaultOidcUser = super.loadUser(userRequest);

    Optional<UserInfo> localUser = userInformationService.findByOidc(defaultOidcUser.getName(),
        defaultOidcUser.getIssuer().toString());
    if (localUser.isPresent()) {
      var user = localUser.get();
      var authorities = userAuthorityProvider.getAuthoritiesByUserId(
          user.id());
      return new QbicOidcUser(authorities, userRequest.getIdToken(),
          defaultOidcUser.getUserInfo(),
          user.id(), user.isActive());
    }
    return defaultOidcUser;
  }
}