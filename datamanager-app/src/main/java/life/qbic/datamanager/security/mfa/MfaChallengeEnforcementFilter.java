package life.qbic.datamanager.security.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import life.qbic.logging.api.Logger;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <b>Belt and braces: no session survives the challenge on a later request (M4-1, M4-5)</b>
 *
 * <p>{@link MfaLoginSuccessHandler} is the primary mechanism and the thing that makes the property
 * true. This filter re-checks it on every request that is <em>not</em> the login POST, so a future
 * addition (another login filter, a remember-me variant, a mis-ordered handler) cannot silently
 * resurrect password-only access. When it fires it removes the authentication and sends the client to
 * the challenge, so the failure mode is "ask for the factor", never "let them in".</p>
 *
 * <p>It is registered <b>before</b> {@code UsernamePasswordAuthenticationFilter}: on the login POST the
 * freshly created authentication must not be stripped by a filter that cannot yet tell whether the
 * success handler will withhold it. Later requests have no such ambiguity.</p>
 *
 * <p>The filter is a no-op unless a pending login exists, so it costs one session lookup per request
 * and nothing when the feature is off.</p>
 *
 * @since 1.0.0
 */
public class MfaChallengeEnforcementFilter extends OncePerRequestFilter {

  private static final Logger log = logger(MfaChallengeEnforcementFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    HttpSession session = request.getSession(false);
    if (session == null || MfaPendingLogin.from(session).isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      // The expected state while a challenge is pending: nothing to strip.
      filterChain.doFilter(request, response);
      return;
    }

    if (isChallengeOrLoginRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    log.warn(
        "An authenticated session existed while a second factor was pending (user %s); "
            + "the authentication has been withdrawn and the challenge is required"
            .formatted(authentication.getName()));
    SecurityContextHolder.clearContext();
    session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    response.sendRedirect(contextPathOf(request) + MfaLoginSuccessHandler.CHALLENGE_PATH);
  }

  private static boolean isChallengeOrLoginRequest(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null) {
      return false;
    }
    String contextPath = contextPathOf(request);
    return path.equals(contextPath + MfaLoginSuccessHandler.CHALLENGE_PATH)
        || path.equals(contextPath + "/login");
  }

  private static String contextPathOf(HttpServletRequest request) {
    return requireNonNull(request.getContextPath() == null ? "" : request.getContextPath());
  }
}
