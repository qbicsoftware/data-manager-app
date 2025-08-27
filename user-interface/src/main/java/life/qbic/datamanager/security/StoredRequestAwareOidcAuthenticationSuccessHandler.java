package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.IssueOidcException;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    if (authFromLinkRequest != null) {
      var returnTo = (String) currentSession.getAttribute(OidcLinkController.RETURN_TO);
      var convertedAuth = (Authentication) authFromLinkRequest;
      // this means the user was already authenticated and triggered the linking of an oidc account
      var originalUserDetails = (QbicUserDetails) convertedAuth.getPrincipal();
      var qbicOidcUser = (QbicOidcUser) authentication.getPrincipal();
      try {
        identityService.setOidc(originalUserDetails.getUserId(), qbicOidcUser.getOidcId(), qbicOidcUser.getOidcIssuer());
        SecurityContextHolder.getContext().setAuthentication(convertedAuth);
        response.sendRedirect(returnTo + "?success=1");
      } catch (IssueOidcException e) {
        logger.error("Error while setting up Oidc Authentication", e);
        SecurityContextHolder.getContext().setAuthentication(convertedAuth);
        response.sendRedirect(returnTo + "?error=" + URLEncoder.encode(e.getMessage(),
            StandardCharsets.UTF_8));
      }
      return;
    }

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
}
