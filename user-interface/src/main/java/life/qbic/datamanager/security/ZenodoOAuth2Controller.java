package life.qbic.datamanager.security;

import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import life.qbic.datamanager.CustomOAuth2AccessTokenResponseClient;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AppContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
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
  private final OAuth2AuthorizedClientManager authorizedClientManager;
  private final ClientRegistrationRepository clientRepo;
  private final CustomOAuth2AccessTokenResponseClient customOAuth2AccessTokenResponseClient;
  private final AppContextProvider appContextProvider;

  @Autowired
  public ZenodoOAuth2Controller(OAuth2AuthorizedClientManager authorizedClientManager,
      ClientRegistrationRepository clientRegistrationRepository,
      CustomOAuth2AccessTokenResponseClient customOAuth2AccessTokenResponseClient,
      AppContextProvider appContextProvider) {
    this.authorizedClientManager = Objects.requireNonNull(authorizedClientManager);
    this.clientRepo = Objects.requireNonNull(clientRegistrationRepository);
    this.customOAuth2AccessTokenResponseClient = Objects.requireNonNull(
        customOAuth2AccessTokenResponseClient);
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
    }

    if (!hasValidState(session, state)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // Retrieve the client registration for Zenodo
    ClientRegistration clientRegistration = clientRepo.findByRegistrationId(ZENODO_CLIENT_ID);
    if (clientRegistration == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Invalid client registration: " + ZENODO_CLIENT_ID);
      return;
    }

    // Build the OAuth2AuthorizationCodeGrantRequest
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

    OAuth2AccessTokenResponse tokenResponse = null;
    var round = 1;
    while (round < 10) {
      try {
        tokenResponse = customOAuth2AccessTokenResponseClient.getTokenResponse(
            authorizationCodeGrantRequest);
      } catch (Exception e) {
        log.error(e.getMessage());
      }
      if (tokenResponse != null) {
        break;
      }
      round++;
      try {
        Thread.sleep(100 * 2 ^ round);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    if (tokenResponse == null) {
      // it failed!
      response.sendRedirect(request.getContextPath() + "/failure");
      return;
    }

    var originalPath = session.getAttribute("datamanager.originalRoute");
    if (originalPath == null) {
      response.sendRedirect(request.getContextPath());
    }
    response.sendRedirect("%s/%s".formatted(request.getContextPath(), originalPath));
  }
}
