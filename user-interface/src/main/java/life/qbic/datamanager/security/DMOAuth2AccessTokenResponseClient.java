package life.qbic.datamanager.security;

import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.TOKEN_TYPE;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.EXPIRES_IN;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
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
public class DMOAuth2AccessTokenResponseClient implements
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

  private static Map<String, Object> filterOptional(Map<String, Object> body) {
    return body.entrySet().stream().filter(entry -> isOptionalParameter(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static boolean isOptionalParameter(String value) {
    return switch (value) {
      case "access_token" -> false;
      case "token_type" -> false;
      case "expires_in" -> false;
      default -> true;
    };
  }

  private static Optional<TokenType> fromString(String tokenType) {
    switch (tokenType.toLowerCase()) {
      case "bearer":
        return Optional.of(TokenType.BEARER);
      default:
        return Optional.empty();
    }
  }

  @Override
  public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
    // Send the token request
    String tokenUri = request.getClientRegistration().getProviderDetails().getTokenUri();
    ResponseEntity<Map<String, Object>> response = new RestTemplate().exchange(
        tokenUri, HttpMethod.POST, createRequestEntity(request),
        new ParameterizedTypeReference<>() {
        }
    );

    // Parse the response manually
    Map<String, Object> body = response.getBody();
    String accessToken = (String) body.get(ACCESS_TOKEN);
    var tokenTypeString = (String) body.get(TOKEN_TYPE);
    TokenType tokenType = fromString((String) body.get(TOKEN_TYPE)).orElseThrow(() ->
        new RuntimeException("Unknown token type: '%s'".formatted(tokenTypeString)));
    long expiresIn = ((Number) body.get(EXPIRES_IN)).longValue();

    return OAuth2AccessTokenResponse.withToken(accessToken)
        .tokenType(tokenType)
        .expiresIn(expiresIn)
        .scopes(request.getClientRegistration().getScopes())
        .additionalParameters(filterOptional(body))
        .build();
  }

  private HttpEntity<?> createRequestEntity(OAuth2AuthorizationCodeGrantRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "authorization_code");
    body.add("code", request.getAuthorizationExchange().getAuthorizationResponse().getCode());
    body.add("redirect_uri",
        request.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
    body.add("client_id", request.getClientRegistration().getClientId());
    body.add("client_secret", request.getClientRegistration().getClientSecret());

    return new HttpEntity<>(body, headers);
  }
}
