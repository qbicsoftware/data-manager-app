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
      var returnTo = (String) Optional.ofNullable(currentSession.getAttribute(OidcLinkController.RETURN_TO)).orElse("");
      var convertedAuth = (Authentication) authFromLinkRequest;
      var originalUserDetails = (QbicUserDetails) convertedAuth.getPrincipal();
      DefaultOidcUser oidcUser;
      // We can only process in the OIDC flow, if the authentication principal is of type DefaultOidcUser
      // Every other principal cannot be processed here and is caught here as fail-safe.
      try {
        oidcUser = (DefaultOidcUser) authentication.getPrincipal();
      } catch (ClassCastException e) {
        // Ensure the original authentication is set in the current context
        SecurityContextHolder.getContext().setAuthentication(convertedAuth);
        cleanUpSession(currentSession);
        response.sendRedirect(returnTo + "/login?error=" + URLEncoder.encode("Something went wrong during authentication.", StandardCharsets.UTF_8));
        return;
      }
      // Only if we have a DefaultOidcUser principal the flow can continue
      try {
        identityService.setOidc(originalUserDetails.getUserId(), oidcUser.getName(), oidcUser.getIssuer().toString());
        SecurityContextHolder.getContext().setAuthentication(convertedAuth);
        response.sendRedirect(returnTo + "?success=1");
      } catch (IssueOidcException e) {
        logger.error("Error while setting up OIDC Authentication", e);
        SecurityContextHolder.getContext().setAuthentication(convertedAuth);
        response.sendRedirect(returnTo + "?error=" + URLEncoder.encode(e.getMessage(),
            StandardCharsets.UTF_8));
      } finally {
        // Clean-up of the session to free the use case flags for linking an Open ID
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

  private static void cleanUpSession(@NonNull HttpSession session) {
    requireNonNull(session);
    // Clean-up of the session to free the use case flags for linking an Open ID
    session.removeAttribute(OidcLinkController.LINK_AUTH_SESSION_KEY);
    session.removeAttribute(OidcLinkController.RETURN_TO);
  }
}
