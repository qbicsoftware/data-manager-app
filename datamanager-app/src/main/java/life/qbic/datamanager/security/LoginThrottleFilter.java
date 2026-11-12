package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <b>Rejects a login attempt before any password work is done, once the account is throttled.</b>
 *
 * <h3>The throttled response must be the same response as a wrong password</h3>
 * <p>AUTH-NFR-03 requires a throttled response to be indistinguishable from an unthrottled one, and
 * decision B4 states the reason explicitly: otherwise the throttle itself becomes the enumeration
 * oracle. Two properties are needed, and both are easy to get wrong:</p>
 * <ol>
 *   <li><b>Same content.</b> The filter does not render its own "too many attempts" page. It hands a
 *       {@link BadCredentialsException} to the <em>same</em> {@link AuthenticationFailureHandler} the
 *       form-login configuration uses, so the visitor lands on the same
 *       {@code /login?error} and sees the same "Incorrect username or password".</li>
 *   <li><b>No faster than the real path.</b> Returning early would make a throttled attempt cheaper
 *       than a wrong password, which is a timing signal that the address is being throttled — and
 *       therefore that it exists. The filter performs one deliberate password derivation so the
 *       rejection costs what a failed authentication costs.</li>
 * </ol>
 *
 * <h3>What it does not do</h3>
 * <p>It does not record the attempt. An attempt rejected here was not a password guess, and counting
 * it would let a blocked client extend its own block by retrying — turning a temporary throttle into
 * a permanent lockout that the attacker controls. Recording happens on a real authentication failure,
 * in {@link AuthAttemptEventListener}.</p>
 *
 * <h3>Scope</h3>
 * <p>Only the password login. The Vaadin login form posts to {@code /login} unless configured
 * otherwise, so guarding that path covers the interactive flow. Resend flows are throttled at their
 * own call sites — see {@code EmailConfirmationMain} — because they are Vaadin round-trips, not form
 * posts, and never reach this filter.</p>
 *
 * @since 1.0.0
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.logger(LoginThrottleFilter.class);

  /** The parameter Spring Security reads the username from on the login POST. */
  static final String USERNAME_PARAMETER = "username";

  private final AuthAttemptThrottle throttle;
  private final AuthenticationFailureHandler failureHandler;
  private final java.util.function.Consumer<String> constantWorkPerformer;

  public LoginThrottleFilter(AuthAttemptThrottle throttle,
      AuthenticationFailureHandler failureHandler,
      java.util.function.Consumer<String> constantWorkPerformer) {
    this.throttle = requireNonNull(throttle, "throttle must not be null");
    this.failureHandler = requireNonNull(failureHandler, "failureHandler must not be null");
    this.constantWorkPerformer = requireNonNull(constantWorkPerformer,
        "constantWorkPerformer must not be null");
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Only the login submission. GET /login must still render the form — throttling a page render
    // would block the very screen the visitor needs to stop failing.
    return !("POST".equalsIgnoreCase(request.getMethod())
        && request.getServletPath().endsWith("/login"));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String address = request.getParameter(USERNAME_PARAMETER);
    if (address == null || address.isBlank()) {
      // Nothing to key on; let Spring Security reject the malformed submission itself.
      filterChain.doFilter(request, response);
      return;
    }

    if (!throttle.isThrottled(AuthAttemptThrottle.Scope.LOGIN, address, Instant.now())) {
      filterChain.doFilter(request, response);
      return;
    }

    // Throttled. Pay the cost of a failed authentication before rejecting, so the response time
    // carries no signal (see property 2 above).
    constantWorkPerformer.accept(address);

    log.info("Rejected a login attempt for a throttled account");
    try {
      failureHandler.onAuthenticationFailure(request, response,
          new BadCredentialsException("Bad credentials"));
    } catch (AuthenticationException e) {
      // AuthenticationFailureHandler declares it may throw; nothing to recover from here, and the
      // handler has already written the redirect by this point.
      log.error("Login failure handler rejected the throttled request", e);
    }
  }
}
