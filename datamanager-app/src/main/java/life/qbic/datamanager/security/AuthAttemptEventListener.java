package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * <b>Counts failed login attempts, and clears the counter on a real success.</b>
 *
 * <h3>Why event listeners rather than a check inside the login form</h3>
 * <p>Spring Security publishes {@link AbstractAuthenticationFailureEvent} for every failed
 * authentication and {@link AuthenticationSuccessEvent} for every success, on every entry point — the
 * Vaadin login form, the {@code /login} POST, and anything added later. Counting here means no
 * authentication path can be forgotten. A check inside the Vaadin form would have covered the form
 * and left the POST endpoint uncounted, which is the same "dead control beside a live feature"
 * failure that caused the previous attempt at this work to be removed rather than shipped.</p>
 *
 * <h3>This class only counts — it does not block</h3>
 * <p>Rejection happens <em>before</em> authentication, in {@link LoginThrottleFilter}. A listener runs
 * after the password has been verified: too late to save the work, and unable to turn a successful
 * authentication back into a failed one. Splitting the two is what lets the throttled response be the
 * same generic failure as a wrong password (AUTH-NFR-03).</p>
 *
 * @since 1.0.0
 */
@Component
public class AuthAttemptEventListener {

  private static final Logger log = LoggerFactory.logger(AuthAttemptEventListener.class);

  private final AuthAttemptThrottle throttle;

  public AuthAttemptEventListener(AuthAttemptThrottle throttle) {
    this.throttle = requireNonNull(throttle, "throttle must not be null");
  }

  /**
   * Clears the bucket for the address after a successful password authentication.
   *
   * <p>Only {@link AuthenticationSuccessEvent} counts. {@link InteractiveAuthenticationSuccessEvent}
   * is a <em>sibling</em> of it in Spring Security 7, not a subclass — verified in the 7.1.2 sources.
   * It is published for success handled by an interactive filter (form login) and is what Vaadin's
   * login flow produces, so both must clear the counter; what must <b>not</b> is a re-authentication
   * that never proved knowledge of the password (remember-me), which publishes
   * {@code RememberMeAuthenticationSuccessEvent}. Excluding the remember-me event by type is
   * deliberate: clearing the counter there would let an attacker reset the bucket without the
   * password.</p>
   */
  @EventListener
  public void onSuccess(AuthenticationSuccessEvent event) {
    String address = event.getAuthentication().getName();
    if (address == null || address.isBlank()) {
      return;
    }
    throttle.onSuccess(AuthAttemptThrottle.Scope.LOGIN, address);
  }

  /**
   * Clears the bucket on an interactive (form-login) success.
   *
   * <p>Separate from {@link #onSuccess(AuthenticationSuccessEvent)} because the two events are
   * siblings, so one handler cannot catch both without also catching remember-me re-authentication.</p>
   */
  @EventListener
  public void onInteractiveSuccess(InteractiveAuthenticationSuccessEvent event) {
    String address = event.getAuthentication().getName();
    if (address == null || address.isBlank()) {
      return;
    }
    throttle.onSuccess(AuthAttemptThrottle.Scope.LOGIN, address);
  }

  /**
   * Records a failed authentication.
   *
   * <p>Record only: the message the visitor sees comes from Spring Security's own handler and is
   * already generic ("Incorrect username or password"). The counter must not add wording.</p>
   */
  @EventListener
  public void onFailure(AbstractAuthenticationFailureEvent event) {
    String address = event.getAuthentication().getName();
    if (address == null || address.isBlank()) {
      return;
    }
    throttle.record(AuthAttemptThrottle.Scope.LOGIN, address, Instant.now());
    // Debug, not info: a failed login is routine, and a log line per failure is both noisy and a
    // second enumeration oracle for anyone who can read logs.
    log.debug("Recorded a failed login attempt");
  }
}
