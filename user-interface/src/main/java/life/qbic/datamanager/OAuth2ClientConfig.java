package life.qbic.datamanager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
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
public class OAuth2ClientConfig {


  public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
    return request -> {
      // Build custom request
      String tokenUri = request.getClientRegistration().getProviderDetails().getTokenUri();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("grant_type", request.getGrantType().getValue());
      body.add("code", request.getAuthorizationExchange().getAuthorizationResponse().getCode());
      body.add("redirect_uri", request.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
      body.add("client_id", request.getClientRegistration().getClientId());
      body.add("client_secret", request.getClientRegistration().getClientSecret());

      HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);
      RestTemplate restTemplate = new RestTemplate();

      ResponseEntity<OAuth2AccessTokenResponse> response = restTemplate.exchange(
          tokenUri, HttpMethod.POST, httpEntity, OAuth2AccessTokenResponse.class);

      return response.getBody();
    };
  }
}
