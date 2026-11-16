package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;

/**
 * <b>Starts the ORCID link (AUTH-R-07).</b>
 *
 * <p>Since ORCID stopped being an identity provider, this endpoint no longer has anything to do with
 * authentication. It records <em>which account</em> is asking to be linked, sends the browser to ORCID,
 * and lets {@link OidcLinkCompletionFilter} finish the job on the way back.</p>
 *
 * <h3>What changed and why</h3>
 * <p>It used to store the caller's {@code Authentication} object in the HTTP session so that a success
 * handler could put it back after the provider round trip. That is gone: the user id is what is actually
 * needed, and a session holding a principal for later restoration is exactly the pattern the
 * second-factor work has to be able to rule out.</p>
 *
 * <h3>Return path</h3>
 * <p>The post-link redirect target arrives as a query parameter, so it is attacker-controllable. It is
 * reduced to a <em>relative, same-origin</em> path: absolute URLs, protocol-relative forms
 * ({@code //host}) and triple-slash forms are rejected, because {@code //evil.example} resolves to a
 * different origin under standard URL resolution.</p>
 *
 * @since 1.11.0
 */
@Controller
public class OidcLinkController {

  private static final Logger log = LoggerFactory.logger(OidcLinkController.class);

  /**
   * Session key holding the account that started a link, or absent when no link is pending.
   *
   * <p>The user id and nothing else — no principal, no token, no assertion.</p>
   */
  public static final String PENDING_LINK = OidcLinkController.class.getName() + ".pendingLinkUserId";

  /** Session key holding the sanitized path to return to after the link. */
  public static final String RETURN_TO = OidcLinkController.class.getName() + ".returnTo";

  public static final String ENDPOINT_LINK_ORCID = "/link/orcid";

  /** Where a completed or refused link lands when no return path was given. */
  private static final String DEFAULT_RETURN = "/settings/profile";

  @GetMapping(ENDPOINT_LINK_ORCID)
  public RedirectView linkOrcid(HttpServletRequest request,
      @RequestParam(name = "return", required = false) String returnParam) {

    Optional<String> userId = authenticatedUserId();
    if (userId.isEmpty()) {
      // Not authenticated: send them to the login form rather than a bare 403, which would read as a
      // broken link. Nothing about the attempt is recorded.
      return new RedirectView(contextPath(request) + "/login");
    }

    HttpSession session = request.getSession(true);
    session.setAttribute(PENDING_LINK, userId.get());
    session.setAttribute(RETURN_TO, safeReturnPath(returnParam));

    return new RedirectView(contextPath(request) + "/oauth2/authorization/orcid");
  }

  /**
   * @return the user id of a password-authenticated caller, or empty for anonymous callers
   */
  private static Optional<String> authenticatedUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return Optional.empty();
    }
    if (authentication.getPrincipal() instanceof QbicUserDetails details) {
      return Optional.of(details.getUserId());
    }
    // Any other principal shape is refused rather than guessed at. Since ORCID is not a login there
    // should be no other shape, and if one appears we want to know about it, not to link an account.
    log.warn("Refused an ORCID link for a principal of type "
        + authentication.getPrincipal().getClass().getSimpleName());
    return Optional.empty();
  }

  /**
   * Reduces the caller-supplied return path to a relative, same-origin path.
   *
   * @param candidate the raw {@code return} parameter, may be {@code null}
   * @return a path beginning with a single {@code /}
   */
  static String safeReturnPath(String candidate) {
    if (candidate == null || candidate.isBlank()) {
      return DEFAULT_RETURN;
    }
    String path = candidate.strip();

    // Reject anything that resolves to another origin. //host and ///host are the interesting cases:
    // a browser treats both as scheme-relative, which a naive "/" prefix check would let through.
    if (path.startsWith("//") || path.startsWith("/\\")) {
      log.warn("Rejected a protocol-relative ORCID link return path");
      return DEFAULT_RETURN;
    }
    if (path.contains("://")) {
      log.warn("Rejected an absolute ORCID link return path");
      return DEFAULT_RETURN;
    }
    try {
      URI uri = new URI(path);
      if (uri.isAbsolute() || uri.getHost() != null) {
        log.warn("Rejected an absolute ORCID link return path");
        return DEFAULT_RETURN;
      }
    } catch (URISyntaxException e) {
      log.warn("Rejected an unparseable ORCID link return path");
      return DEFAULT_RETURN;
    }
    if (!path.startsWith("/")) {
      return "/" + path;
    }
    return path;
  }

  private static String contextPath(HttpServletRequest request) {
    String context = request.getContextPath();
    return requireNonNull(context == null ? "" : context);
  }
}
