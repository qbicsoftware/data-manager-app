package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import org.springframework.security.core.Authentication;
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

  public StoredRequestAwareOidcAuthenticationSuccessHandler(String openIdRegistrationEndpoint) {
    this.openIdRegistrationEndpoint = requireNonNull(openIdRegistrationEndpoint,
        "openIdRegistrationEndpoint must not be null");

  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws ServletException, IOException {
    if (authentication.getPrincipal() instanceof QbicOidcUser) {
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
