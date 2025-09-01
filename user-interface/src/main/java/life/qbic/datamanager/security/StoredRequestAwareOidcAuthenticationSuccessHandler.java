package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.IssueOidcException;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Handles action on successful authentication with an OpenId Connect Provider. On successfull
 * authentication, reroutes to registration if no user account was found. Else navigates to the
 * saved request
 *
 * @see SavedRequestAwareAuthenticationSuccessHandler
 * @since 1.1.0
 */
public class StoredRequestAwareOidcAuthenticationSuccessHandler extends
    VaadinSavedRequestAwareAuthenticationSuccessHandler {

  public static final String GENERAL_AUTHENTICATION_FAILURE = "Something went wrong during authentication.";
  private final String openIdRegistrationEndpoint;
  private final String emailConfirmationEndpoint;
  private final IdentityService identityService;

  public StoredRequestAwareOidcAuthenticationSuccessHandler(
      String openIdRegistrationEndpoint,
      String emailConfirmationEndpoint,
      IdentityService identityService) {
    this.openIdRegistrationEndpoint = requireNonNull(openIdRegistrationEndpoint,
        "openIdRegistrationEndpoint must not be null");
    this.emailConfirmationEndpoint = requireNonNull(emailConfirmationEndpoint,
        "emailConfirmationEndpoint must not be null");
    this.identityService = requireNonNull(identityService);
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws ServletException, IOException {
    var currentSession = request.getSession();
    var authFromLinkRequest = currentSession.getAttribute(OidcLinkController.LINK_AUTH_SESSION_KEY);
    // Check for the OIDC link use case (using the OpenID to authenticate for the Data Manager account)
    if (authFromLinkRequest != null) {
      // the user was already authenticated and wants to link their OpenID account
      var returnTo = (String) Optional.ofNullable(
          currentSession.getAttribute(OidcLinkController.RETURN_TO)).orElse("");
      if (!(authFromLinkRequest instanceof Authentication previousAuth)) {
        logger.error("Unknown authentication type: %s".formatted(authFromLinkRequest.getClass()));
        cleanUpSession(currentSession);
        response.sendRedirect(request.getContextPath() + "/login?error=" + URLEncoder.encode(
            GENERAL_AUTHENTICATION_FAILURE, StandardCharsets.UTF_8));
        return;
      }

      if (!(previousAuth.getPrincipal() instanceof QbicUserDetails originalUserDetails)) {
        var actualInstance = previousAuth.getPrincipal().getClass();
        throw new IllegalStateException(
            "Provided principal did not meet requirements. Expected %s but received %s".formatted(
                QbicUserDetails.class, actualInstance));
      }

      OidcInfo oidcInfo = switch (authentication.getPrincipal()) {
        case QbicOidcUser user -> fromQbicOidc(user);
        case DefaultOidcUser user -> fromDefaultOidc(user);
        default -> null;
      };

      try {
        Optional.ofNullable(oidcInfo)
            .orElseThrow(() -> new IllegalArgumentException("OidcInfo is null"));
        identityService.setOidc(originalUserDetails.getUserId(), oidcInfo.id(),
            oidcInfo.oidcIssuer());
        SecurityContextHolder.getContext().setAuthentication(previousAuth);
        response.sendRedirect(returnTo + "?success=1");
      } catch (IssueOidcException e) {
        logger.error("Error while setting up OIDC Authentication", e);
        SecurityContextHolder.getContext().setAuthentication(previousAuth);
        response.sendRedirect(returnTo + "?error=" + URLEncoder.encode(e.getMessage(),
            StandardCharsets.UTF_8));
      } catch (IllegalArgumentException e) {
        logger.error("Error while setting up OIDC Authentication", e);
        SecurityContextHolder.getContext().setAuthentication(previousAuth);
        response.sendRedirect(returnTo + "?error=" + URLEncoder.encode(
            GENERAL_AUTHENTICATION_FAILURE, StandardCharsets.UTF_8));
      } finally {
        cleanUpSession(currentSession);
      }
      return;
    }

    // Legacy authentication flow of the application
    if (authentication.getPrincipal() instanceof QbicOidcUser qbicOidcUser) {
      if (!qbicOidcUser.isActive()) {
        getRedirectStrategy().sendRedirect(request, response, emailConfirmationEndpoint);
        return;
      }
      super.onAuthenticationSuccess(request, response, authentication);
    } else if (authentication.getPrincipal() instanceof OidcUser) {
      getRedirectStrategy().sendRedirect(request, response, openIdRegistrationEndpoint);
    } else {
      //we do not redirect
      logger.error(
          "Authentication failure. Unsupported principal type: " + authentication.getPrincipal()
              .getClass());
    }
  }

  /**
   * Cleans the session from outdated attributes
   *
   * @param session the session to clean up
   * @since 1.11.0
   */
  private static void cleanUpSession(@NonNull HttpSession session) {
    requireNonNull(session);
    session.removeAttribute(OidcLinkController.LINK_AUTH_SESSION_KEY);
    session.removeAttribute(OidcLinkController.RETURN_TO);
  }

  record OidcInfo(String id, String oidcIssuer) {

  }

  private static OidcInfo fromDefaultOidc(DefaultOidcUser user) {
    return new OidcInfo(user.getName(), user.getIssuer().toString());
  }

  private static OidcInfo fromQbicOidc(QbicOidcUser user) {
    return new OidcInfo(user.getOidcId(), user.getOidcIssuer());
  }
}
