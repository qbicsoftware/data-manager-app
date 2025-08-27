package life.qbic.datamanager.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Controller
public class OidcLinkController {

  public static final String LINK_AUTH_SESSION_KEY = "linking.originalAuth";
  public static final String RETURN_TO = "linking.returnTo";

  @GetMapping("/link/orcid")
  public RedirectView linkOrcid(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return new RedirectView("/login");
    }
    HttpSession session = request.getSession(true);

    session.setAttribute(LINK_AUTH_SESSION_KEY, auth);
    session.setAttribute(RETURN_TO, sanitizeUrl("/profile", request.getContextPath()));

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
