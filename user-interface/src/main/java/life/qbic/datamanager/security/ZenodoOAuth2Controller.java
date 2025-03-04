package life.qbic.datamanager.security;

import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import life.qbic.datamanager.security.context.DMOAuth2BearerToken;
import life.qbic.datamanager.security.context.DMSecurityContext;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AppContextProvider;
import org.apache.catalina.util.URLEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@RestController
@RequestMapping("/zenodo")
public class ZenodoOAuth2Controller {

  public static final String ZENODO_CLIENT_ID = "zenodo";
  public static final int STATE_LENGTH = 32;
  private static final Logger log = logger(ZenodoOAuth2Controller.class);
  private final ClientRegistrationRepository clientRepo;
  private final DMOAuth2AccessTokenResponseClient dmOAuth2AccessTokenResponseClient;
  private final AppContextProvider appContextProvider;

  @Autowired
  public ZenodoOAuth2Controller(ClientRegistrationRepository clientRegistrationRepository,
      DMOAuth2AccessTokenResponseClient dmOAuth2AccessTokenResponseClient,
      AppContextProvider appContextProvider) {
    this.clientRepo = Objects.requireNonNull(clientRegistrationRepository);
    this.dmOAuth2AccessTokenResponseClient = Objects.requireNonNull(
        dmOAuth2AccessTokenResponseClient);
    this.appContextProvider = Objects.requireNonNull(appContextProvider);
  }

  private static boolean hasValidState(HttpSession session, String responseState) {
    var request = (OAuth2AuthorizationRequest) session.getAttribute(
        HttpSessionOAuth2AuthorizationRequestRepository.class.getName() + ".AUTHORIZATION_REQUEST");
    if (request == null) {
      return false;
    }
    return request.getState().equals(responseState);
  }

  private static String secureRandomString() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[STATE_LENGTH];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().encodeToString(bytes);
  }

  @GetMapping("/callback")
  public void handleZenodoCallback(HttpServletRequest request, @RequestParam("code") String code,
      @RequestParam("state") String state, HttpServletResponse response)
      throws IOException {
    var session = request.getSession(false);
    if (session == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    if (!hasValidState(session, state)) { // CSRF protection
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    // Retrieve the client registration for Zenodo
    ClientRegistration clientRegistration = clientRepo.findByRegistrationId(ZENODO_CLIENT_ID);
    if (clientRegistration == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Invalid client registration: " + ZENODO_CLIENT_ID);
      return;
    }

    // Build the request for the access token
    OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest = new OAuth2AuthorizationCodeGrantRequest(
        clientRegistration,
        new OAuth2AuthorizationExchange(
            OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .clientId(clientRegistration.getClientId())
                .redirectUri(clientRegistration.getRedirectUri()
                    .replace("{baseUrl}", appContextProvider.baseUrl()))
                .state(secureRandomString()) // Use a real state value
                .build(),
            OAuth2AuthorizationResponse.success(code)
                .code(code)
                .redirectUri(clientRegistration.getRedirectUri()
                    .replace("{baseUrl}", appContextProvider.baseUrl()))
                .build()
        )
    );

    // Execute the request via our custom response client, in order to conform to Zenodo's auth API.
    OAuth2AccessTokenResponse tokenResponse = null;
    var round = 1;
    while (round < 10) {
      try {
        tokenResponse = dmOAuth2AccessTokenResponseClient.getTokenResponse(
            authorizationCodeGrantRequest);
      } catch (Exception e) {
        log.error(e.getMessage());
      }
      if (tokenResponse != null) {
        break;
      }
      round++;
      try {
        Thread.sleep(100 * 2 ^ round); // we increase the duration every time to cut the server some slack
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    var originalPath = session.getAttribute("datamanager.originalRoute");

    if (tokenResponse == null) {
      // it failed!
      if (originalPath != null) {
        response.sendRedirect("%s/%s?error=%s".formatted(request.getContextPath(), originalPath, new URLEncoder().encode(
            OAuth2Error.AUTH_FAILED, StandardCharsets.UTF_8)));
      } else {
        response.sendRedirect("%s?error=%s".formatted(request.getContextPath(), new URLEncoder().encode(
            OAuth2Error.AUTH_FAILED, StandardCharsets.UTF_8)));
      }
      return;
    }

    DMSecurityContext context;
    if (session.getAttribute(DMSecurityContext.NAME) != null) {
      var existingContext = (DMSecurityContext) session.getAttribute(DMSecurityContext.NAME);
      context = createSecurityContext(tokenResponse, existingContext.principals().toArray());
    } else {
      context = createSecurityContext(tokenResponse);
    }

    session.setAttribute(DMSecurityContext.NAME, context);

    if (originalPath == null) {
      response.sendRedirect(request.getContextPath());
    }
    response.sendRedirect("%s/%s".formatted(request.getContextPath(), originalPath));
  }

  private static DMSecurityContext createSecurityContext(OAuth2AccessTokenResponse tokenResponse) {
    var dmToken = new DMOAuth2BearerToken(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), "zenodo");
    var contextBuilder = new DMSecurityContext.Builder();
    return contextBuilder.addPrincipal(dmToken).build();
  }

  private static DMSecurityContext createSecurityContext(OAuth2AccessTokenResponse tokenResponse, Object... principals) {
    var dmToken = new DMOAuth2BearerToken(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), "zenodo");
    var contextBuilder = new DMSecurityContext.Builder();
    contextBuilder.addPrincipal(dmToken);
    Arrays.stream(principals).forEach(contextBuilder::addPrincipal);
    return contextBuilder.build();
  }

  private static class OAuth2Error {
    static final String AUTH_FAILED = "auth_failed";
  }
}
