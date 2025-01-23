package life.qbic.datamanager.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class ZenodoOAuth2Service {

  public static final String ZENODO = "zenodo";
  private final OAuth2AuthorizedClientManager authorizedClientManager;

  @Autowired
  public ZenodoOAuth2Service(OAuth2AuthorizedClientManager authorizedClientManager,
      ClientRegistrationRepository clientRegistrationRepository) {
    this.authorizedClientManager = Objects.requireNonNull(authorizedClientManager);
    Objects.requireNonNull(clientRegistrationRepository.findByRegistrationId(ZENODO));
  }

  public OAuth2AccessToken accessToken(Authentication principal, HttpServletResponse response)
      throws IOException {
    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("zenodo")
        .principal(principal)
        .build();

    try {
      OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
      if (authorizedClient != null) {
        return authorizedClient.getAccessToken();
      }
    } catch (OAuth2AuthorizationException ex) {
      // Trigger Zenodo authorization
      String authorizationUri = ex.getError().getUri();
      response.sendRedirect(authorizationUri);
    }
    return null;
  }

}
