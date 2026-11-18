package life.qbic.datamanager.security.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;

/**
 * <b>Stops the password from being a login (AUTH-R-01)</b>
 *
 * <p>Runs after the password was verified and decides whether the session may exist. For an account
 * that must present a factor, the authentication is <em>removed</em> — from the thread and from the
 * session — and only a {@link MfaPendingLogin} is left behind. The user then has five minutes to
 * present a code; the password alone has established nothing.</p>
 *
 * <h3>Why the authentication has to be removed twice</h3>
 * <p>Spring's {@code AbstractAuthenticationProcessingFilter} saves the security context into the
 * session <em>before</em> it calls the success handler. Clearing {@link SecurityContextHolder} alone
 * would leave the saved context in the session, and the very next request would restore a fully
 * authenticated session — the bypass this handler exists to prevent. Both the holder and the session
 * attribute are therefore cleared. That is also why the assertion in the bypass suite (M4-5) is
 * "no authenticated session exists after the password POST", not "the redirect points at the
 * challenge".</p>
 *
 * <p><b>Defence in depth:</b> {@link MfaChallengeEnforcementFilter} re-checks the same property on
 * every subsequent request, so a future change that bypasses this handler (a second login filter, a
 * remember-me variant) cannot silently restore password-only access.</p>
 *
 * @since 1.0.0
 */
@Component
public class MfaLoginSuccessHandler implements AuthenticationSuccessHandler {

  private static final Logger log = logger(MfaLoginSuccessHandler.class);

  static final String CHALLENGE_PATH = "/login/second-factor";

  private final MfaLoginGuard loginGuard;
  private final AuthenticationSuccessHandler delegate;

  public MfaLoginSuccessHandler(MfaLoginGuard loginGuard, RequestCache requestCache) {
    this(loginGuard, requestCache, savedRequestAware(requestCache));
  }

  /** Visible for tests: lets a spec observe whether the normal success path ran. */
  MfaLoginSuccessHandler(MfaLoginGuard loginGuard, RequestCache requestCache,
      AuthenticationSuccessHandler delegate) {
    this.loginGuard = requireNonNull(loginGuard, "loginGuard must not be null");
    this.delegate = requireNonNull(delegate, "delegate must not be null");
  }

  private static AuthenticationSuccessHandler savedRequestAware(RequestCache requestCache) {
    var handler = new SavedRequestAwareAuthenticationSuccessHandler();
    handler.setRequestCache(requireNonNull(requestCache, "requestCache must not be null"));
    handler.setDefaultTargetUrl("/");
    return handler;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    QbicUserDetails principal = principalOf(authentication);
    MfaGateDecision decision = loginGuard.decide(principal.getUserId(), Instant.now());

    switch (decision) {
      case MfaGateDecision.Challenge challenge -> {
        startChallenge(request, response, principal);
      }
      case MfaGateDecision.GraceExpired expired -> {
        // The account is past its enrolment window. Deny, and say what to do — the password was
        // correct, so this is not an enumeration oracle; it is the account owner being told why
        // they cannot get in, before they call support anyway.
        forgetAuthentication(request);
        log.info("Login denied for user %s: second-factor grace expired at %s"
            .formatted(principal.getUserId(), expired.graceUntil()));
        response.sendRedirect(contextPathOf(request) + CHALLENGE_PATH + "?graceExpired");
      }
      // NotRequired / GraceGranted / GraceActive: the session may exist. GraceGranted was already
      // persisted by the guard, so the account has a deadline from now on.
      default -> delegate.onAuthenticationSuccess(request, response, authentication);
    }
  }

  private void startChallenge(HttpServletRequest request, HttpServletResponse response,
      QbicUserDetails principal) throws IOException {
    // getUsername() returns the USER ID (ACL records are keyed on it); the email is the login name
    // the challenge needs to reload the principal, and it is only reachable via getEmailAddress().
    MfaPendingLogin.store(request,
        new MfaPendingLogin(principal.getUserId(), principal.getEmailAddress(), Instant.now()));
    forgetAuthentication(request);
    log.info("Second factor required for user %s; authentication withheld"
        .formatted(principal.getUserId()));
    response.sendRedirect(contextPathOf(request) + CHALLENGE_PATH);
  }

  /**
   * Removes the authentication from the thread <b>and</b> from the session.
   *
   * <p>This method is the security property of the whole class: see the class documentation for why
   * both are required.</p>
   */
  private static void forgetAuthentication(HttpServletRequest request) {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }
  }

  private static QbicUserDetails principalOf(Authentication authentication) {
    if (authentication == null
        || !(authentication.getPrincipal() instanceof QbicUserDetails details)) {
      // Anything else means the chain was changed in a way this handler does not understand. Fail
      // closed: the login is refused rather than silently treated as "no factor needed".
      throw new IllegalStateException(
          "second-factor login handler received an unexpected authentication principal");
    }
    return details;
  }

  private static String contextPathOf(HttpServletRequest request) {
    return request.getContextPath() == null ? "" : request.getContextPath();
  }
}
