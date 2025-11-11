package life.qbic.datamanager.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/**
 * <b>OpenID Link Controller</b>
 * <p>
 * A controller that provides an endpoint to enable the OpenID link use case. This use case is
 * executed when users want to link an existing Data Manager account with their OpenID, e.g. ORCiD.
 * <p>
 * This way, the account is enriched with the OpenID identifier (e.g. ORCiD ID) and the issuer
 * itself, which enables user to log into their account with their OpenID account instead of a local
 * account.
 *
 * @since 1.11.0
 */
@Controller
public class OidcLinkController {

  public static final String LINK_AUTH_SESSION_KEY = "linking.originalAuth";
  public static final String RETURN_TO = "linking.returnTo";

  public static final String ENDPOINT_LINK_ORCID = "/link/orcid";

  @GetMapping("/link/orcid")
  public RedirectView linkOrcid(HttpServletRequest request,
      @RequestParam(name = "return", required = false) String returnParam) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return new RedirectView("/login");
    }
    HttpSession session = request.getSession(true);

    var returnPath = Optional.ofNullable(returnParam).orElse("");

    session.setAttribute(LINK_AUTH_SESSION_KEY, auth);
    session.setAttribute(RETURN_TO, sanitizeUrl("/" + returnPath, request.getContextPath()));

    return new RedirectView(sanitizeUrl("/oauth2/authorization/orcid", request.getContextPath()));
  }

  private static String sanitizeUrl(String candidate, String context) {
    if (context == null || context.isEmpty()) {
      return candidate;
    }
    if ("/".equals(context)) {
      return candidate;
    }
    return context + candidate;
  }
}
