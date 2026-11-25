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
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import org.springframework.beans.factory.annotation.Autowired;
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

  /**
   * Where an account that is not owed a grace window must go instead of a session (registration path).
   *
   * <p>Declared here, next to the challenge, because both are outcomes of the same decision and the
   * filter reuses them. The value matches {@code RequiredEnrolmentView.PATH}; the view is not a
   * dependency of this layer, so the constant is duplicated rather than imported — and a test asserts
   * the two agree, because a redirect to a route that does not exist is a silent 404 at login.</p>
   */
  public static final String ENROL_PATH = "/login/enrol";

  private final MfaLoginGuard loginGuard;
  private final AuthenticationSuccessHandler delegate;

  /**
   * Spring's constructor. Explicit {@code @Autowired} because the class has a second, package-private
   * constructor for tests; without it Spring finds two candidates and falls back to looking for a
   * no-arg constructor, which fails at startup.
   *
   * <p>The cache is {@link VaadinDefaultRequestCache} rather than the {@code RequestCache} interface on
   * purpose: it is the very instance the filter chain uses, so the challenge and the login page agree
   * on where the user was going.</p>
   */
  @Autowired
  public MfaLoginSuccessHandler(MfaLoginGuard loginGuard, VaadinDefaultRequestCache requestCache) {
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

    // A fresh password authentication invalidates whatever a previous login left half-finished in
    // this session. Without this, the "Use a different account" link on the challenge page is an
    // identity substitution (MFA-25): account A is challenged, the visitor goes back, signs in as the
    // factor-free account B, and the default branch below then stores B's session marker while A's
    // pending login survives — so a code from A's authenticator completes the challenge as A.
    //
    // Both attributes go: the pending login names the account that must present the factor, and the
    // marker answers "this session passed the step" for the account it was issued to. Neither may
    // outlive the login that produced them.
    discardStaleLoginState(request);

    MfaGateDecision decision = loginGuard.decide(principal.getUserId(), Instant.now());

    switch (decision) {
      case MfaGateDecision.Challenge challenge -> {
        startChallenge(request, response, principal);
      }
      case MfaGateDecision.EnrolmentDue due -> {
        // The account is not owed a deadline, so there is nothing to count down: it enrols now or it
        // does not get in. Same mechanical step as the challenge — the password established nothing.
        startChallenge(request, response, principal, ENROL_PATH);
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
      default -> {
        // Remember the answer for this session so the enforcement filter does not have to ask the
        // database on every request.
        MfaSessionState.store(request.getSession(false), principal.getUserId(), Instant.now());
        delegate.onAuthenticationSuccess(request, response, authentication);
      }
    }
  }

  private void startChallenge(HttpServletRequest request, HttpServletResponse response,
      QbicUserDetails principal) throws IOException {
    startChallenge(request, response, principal, CHALLENGE_PATH);
  }

  /**
   * Leaves the session with nothing but a pending login and sends the browser to the step that
   * completes it.
   *
   * @param target the route to send the user to: the challenge, or mandatory enrolment
   */
  private void startChallenge(HttpServletRequest request, HttpServletResponse response,
      QbicUserDetails principal, String target) throws IOException {
    // getUsername() returns the USER ID (ACL records are keyed on it); the email is the login name
    // the challenge needs to reload the principal, and it is only reachable via getEmailAddress().
    MfaPendingLogin.store(request,
        new MfaPendingLogin(principal.getUserId(), principal.getEmailAddress(), Instant.now()));
    forgetAuthentication(request);
    log.info("Second factor required for user %s; authentication withheld, target %s"
        .formatted(principal.getUserId(), target));
    response.sendRedirect(contextPathOf(request) + target);
  }

  /**
   * Clears the two session attributes that describe an in-flight or completed login.
   *
   * <p>Called before the gate decides, so every branch — challenge, enrolment, grace, and the plain
   * allow — starts from a session that describes only the login being handled now.</p>
   */
  private static void discardStaleLoginState(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return;
    }
    MfaPendingLogin.clear(session);
    MfaSessionState.clear(session);
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
