package life.qbic.datamanager.security.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import life.qbic.identity.application.mfa.MfaLoginGuard;
import life.qbic.identity.domain.model.mfa.MfaGateDecision;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <b>Enforces the second factor on every request, independent of the login configuration (M4-1)</b>
 *
 * <p>The success handler is the intended place to withhold the authentication, but it is only reached
 * if the login filter chain keeps it — and a framework configurer can replace it. That is exactly what
 * happened on the first real deployment: the policy was {@code OPTIONAL}, the account was enrolled, and
 * nothing was asked. A security control must not depend on winning that argument.</p>
 *
 * <p>So the rule is enforced here, on every authenticated request:</p>
 * <ol>
 *   <li>no authenticated principal → pass (login, challenge and anonymous traffic live downstream);</li>
 *   <li>this session already passed the step for that account ({@link MfaSessionState}) → pass, which
 *       is the normal case and costs one session lookup;</li>
 *   <li>a pending login exists → the password produced a session anyway, so strip it and send the user
 *       to the challenge;</li>
 *   <li>otherwise ask {@link MfaLoginGuard} once: {@code Challenge} strips the session and starts the
 *       challenge, {@code GraceExpired} denies, and everything else marks the session satisfied.</li>
 * </ol>
 *
 * <p><b>Why not simply use a success handler only:</b> because "the challenge did not appear" is
 * indistinguishable from "no factor is configured" — the failure mode is silent. A filter that runs on
 * every request makes the property hold no matter which login path created the session, including a
 * future remember-me or a second filter chain.</p>
 *
 * <p>Registered <b>before</b> {@code UsernamePasswordAuthenticationFilter}: on the login POST the
 * freshly created authentication belongs to the success handler, which may withhold it with more
 * context than this filter has.</p>
 *
 * @since 1.0.0
 */
public class MfaChallengeEnforcementFilter extends OncePerRequestFilter {

  private static final Logger log = logger(MfaChallengeEnforcementFilter.class);

  private final MfaLoginGuard loginGuard;

  public MfaChallengeEnforcementFilter(MfaLoginGuard loginGuard) {
    this.loginGuard = requireNonNull(loginGuard, "loginGuard must not be null");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    HttpSession session = request.getSession(false);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (session == null
        || authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken
        || isChallengeOrLoginRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    String userId = userIdOf(authentication);
    if (userId == null || MfaSessionState.satisfies(session, userId)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (MfaPendingLogin.from(session).isPresent()) {
      // The password produced an authenticated session even though a factor is pending. Withdraw it.
      log.warn(("An authenticated session existed while a second factor was pending (user %s); "
          + "the authentication has been withdrawn and the challenge is required")
          .formatted(userId));
      withdraw(session);
      response.sendRedirect(contextPathOf(request) + MfaLoginSuccessHandler.CHALLENGE_PATH);
      return;
    }

    MfaGateDecision decision = loginGuard.decide(userId, Instant.now());
    switch (decision) {
      case MfaGateDecision.Challenge challenge -> {
        log.info("Second factor required for user %s; session withheld at the request boundary"
            .formatted(userId));
        MfaPendingLogin.store(request, new MfaPendingLogin(userId, emailOf(authentication),
            Instant.now()));
        withdraw(session);
        response.sendRedirect(contextPathOf(request) + MfaLoginSuccessHandler.CHALLENGE_PATH);
      }
      case MfaGateDecision.GraceExpired expired -> {
        log.info("Login denied for user %s: second-factor grace expired at %s"
            .formatted(userId, expired.graceUntil()));
        withdraw(session);
        response.sendRedirect(
            contextPathOf(request) + MfaLoginSuccessHandler.CHALLENGE_PATH + "?graceExpired");
      }
      // NotRequired / GraceGranted / GraceActive: this account may use the session. GraceGranted was
      // already persisted by the guard, so it has a deadline from now on.
      default -> {
        MfaSessionState.store(session, userId, Instant.now());
        filterChain.doFilter(request, response);
      }
    }
  }

  /** Removes the authentication from the thread and from the session (see the success handler). */
  private static void withdraw(HttpSession session) {
    SecurityContextHolder.clearContext();
    session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
  }

  private static String userIdOf(Authentication authentication) {
    return authentication.getPrincipal() instanceof QbicUserDetails details
        ? details.getUserId()
        : null;
  }

  private static String emailOf(Authentication authentication) {
    return authentication.getPrincipal() instanceof QbicUserDetails details
        ? details.getEmailAddress()
        : "";
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
    String contextPath = request.getContextPath();
    return contextPath == null ? "" : contextPath;
  }
}
