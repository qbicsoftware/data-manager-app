package life.qbic.datamanager;

import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class CustomOAuth2AccessTokenResponseClient implements
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
    // Send the token request
    String tokenUri = request.getClientRegistration().getProviderDetails().getTokenUri();
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        tokenUri, HttpMethod.POST, createRequestEntity(request),
        new ParameterizedTypeReference<>() {}
    );

    // Parse the response manually
    Map<String, Object> body = response.getBody();
    String accessToken = (String) body.get("access_token");
    String tokenType = (String) body.get("token_type");
    Long expiresIn = ((Number) body.get("expires_in")).longValue();

    return OAuth2AccessTokenResponse.withToken(accessToken)
        .tokenType(OAuth2AccessToken.TokenType.BEARER)
        .expiresIn(expiresIn)
        .scopes(request.getClientRegistration().getScopes())
        .build();
  }

  private HttpEntity<?> createRequestEntity(OAuth2AuthorizationCodeGrantRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "authorization_code");
    body.add("code", request.getAuthorizationExchange().getAuthorizationResponse().getCode());
    body.add("redirect_uri", request.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
    body.add("client_id", request.getClientRegistration().getClientId());
    body.add("client_secret", request.getClientRegistration().getClientSecret());

    return new HttpEntity<>(body, headers);
  }
}
