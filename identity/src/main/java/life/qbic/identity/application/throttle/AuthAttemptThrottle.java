package life.qbic.identity.application.throttle;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import life.qbic.identity.domain.repository.AuthAttemptCounter;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b>Server-side throttling for the unauthenticated authentication flows.</b>
 *
 * <p>Implements the throttling clause of AUTH-NFR-03 for login, resend-confirmation and
 * resend-reset. Replaces the client-side 30-second {@code Timer} in the confirmation view, which a
 * page reload reset and which therefore was not a control at all.</p>
 *
 * <h3>Rule 1 — the counter is keyed on a hash, and there is no account lookup</h3>
 * <p>{@link #record(Scope, String, Instant)} never asks whether the address exists. It cannot: the
 * key is derived from the string the visitor typed. That is deliberate, because a throttle that
 * behaves differently for a registered and an unregistered address becomes the account-enumeration
 * oracle AUTH-NFR-03 forbids — and the throttle is consulted on every request, including requests
 * whose account is unknown.</p>
 *
 * <h3>Rule 2 — a throttled outcome must be indistinguishable from a normal one</h3>
 * <p>{@link #record} returns a {@link Decision}, and <b>every caller must ignore which one it got
 * when composing its response.</b> A caller that renders "too many attempts" tells an attacker that
 * their guess reached the threshold, and — because the threshold is per account — lets them confirm
 * that an address they are spamming is real. The decision exists to decide whether to <em>perform the
 * work</em>, not what to say.</p>
 *
 * <p>This is a sharp edge, so it is stated in the type: {@link Decision#isThrottled()} is for control
 * flow inside the server, never for the response.</p>
 *
 * <h3>Rule 3 — count per account, never per account alone</h3>
 * <p>Per-account-only throttling hands an attacker a denial of service against a chosen victim: spam
 * their address, and they cannot log in. The requirement therefore asks for account <em>and</em>
 * origin. Origin scope is not implemented here — see
 * {@code sql/migrations/auth-attempt-throttling.sql} for why (the client address is the HAProxy, so
 * an origin bucket would be global) — and {@link Scope} is an open set so it can be added without a
 * schema change.</p>
 *
 * <h3>Rule 4 — thresholds are deployment configuration</h3>
 * <p>Nothing here is a constant. {@code qbic.auth-throttle.*} supplies the limits, because they are
 * tuned from telemetry and differ per environment.</p>
 *
 * @since 1.0.0
 */
@Service
public class AuthAttemptThrottle {

  private static final Logger log = LoggerFactory.logger(AuthAttemptThrottle.class);

  /**
   * The throttled flows. Each is an independent bucket, so exhausting the confirmation-resend limit
   * does not lock the visitor out of login.
   *
   * <p>An open set rather than a closed enum of the whole auth surface, so second-factor throttling
   * (MILESTONE-MFA M5-4) reuses this mechanism instead of adding a second table.</p>
   */
  public enum Scope {
    LOGIN,
    EMAIL_CONFIRMATION_RESEND,
    PASSWORD_RESET_RESEND
  }

  /** Whether the caller may perform the work. Never render this; see Rule 2. */
  public enum Decision {
    ALLOWED,
    THROTTLED
  }

  private final AuthAttemptCounter counter;
  private final Policy loginPolicy;
  private final Policy resendPolicy;

  public AuthAttemptThrottle(AuthAttemptCounter counter,
      @Value("${qbic.auth-throttle.login.max-attempts:10}") int loginMaxAttempts,
      @Value("${qbic.auth-throttle.login.window:PT15M}") Duration loginWindow,
      @Value("${qbic.auth-throttle.login.block-duration:PT15M}") Duration loginBlockDuration,
      @Value("${qbic.auth-throttle.resend.max-attempts:3}") int resendMaxAttempts,
      @Value("${qbic.auth-throttle.resend.window:PT15M}") Duration resendWindow,
      @Value("${qbic.auth-throttle.resend.block-duration:PT15M}") Duration resendBlockDuration) {
    this.counter = requireNonNull(counter, "counter must not be null");
    this.loginPolicy = new Policy(loginMaxAttempts, loginWindow, loginBlockDuration, "login");
    this.resendPolicy = new Policy(resendMaxAttempts, resendWindow, resendBlockDuration, "resend");
  }

  /**
   * Records an attempt and reports whether the caller may perform the work.
   *
   * <p>The attempt is recorded even when it is already blocked, so a client that keeps hammering
   * does not shorten its own block by noticing that the count stopped moving.</p>
   *
   * @param scope the flow
   * @param identifier the address as typed; normalised and hashed here, never stored raw
   * @param now   the attempt instant
   * @return whether to perform the work. <b>Do not render this.</b>
   */
  public Decision record(Scope scope, String identifier, Instant now) {
    requireNonNull(scope, "scope must not be null");
    requireNonNull(now, "now must not be null");
    if (identifier == null || identifier.isBlank()) {
      // Nothing to key on. Allowing is correct: the request carries no address, so there is no
      // bucket to fill and no account to attack. Callers validate the address separately.
      return Decision.ALLOWED;
    }

    Policy policy = policyFor(scope);
    String key = attemptKey(scope, identifier);

    // ONE increment, always. Splitting this into "check whether blocked, then increment" would
    // count a throttled attempt zero times, letting an already-blocked client keep its count — and
    // would also double-increment, because the check itself writes.
    Instant windowStart = now.minus(policy.window());
    AuthAttemptCounter.Counter state = counter.increment(scope.name(), key, now, windowStart);

    // The stored block is evaluated against THIS clock, never with NOW() in SQL — every column is
    // datetime(6) with no zone, so a SQL-side comparison silently inherits the session's offset.
    if (state.blockedUntil() != null && now.isBefore(state.blockedUntil())) {
      return Decision.THROTTLED;
    }
    if (state.attemptCount() > policy.maxAttempts()) {
      // Crossed the threshold. Arm the block, then report throttled for this request too: the
      // attempt that crosses the line is itself over the limit.
      counter.block(scope.name(), key, now.plus(policy.blockDuration()));
      log.info("Throttling %s attempts after %d attempts in the window"
          .formatted(scope, state.attemptCount()));
      return Decision.THROTTLED;
    }
    return Decision.ALLOWED;
  }

  /**
   * Reports whether this identifier is currently blocked, <b>without recording an attempt</b>.
   *
   * <p>Called before authentication is attempted. The result is for control flow only — the caller
   * must produce the <em>same</em> generic failure it would have produced anyway (see Rule 2).</p>
   *
   * @param scope      the flow
   * @param identifier the address as typed
   * @param now        the instant to evaluate against
   * @return {@code true} if the caller should skip the work
   */
  public boolean isThrottled(Scope scope, String identifier, Instant now) {
    requireNonNull(scope, "scope must not be null");
    requireNonNull(now, "now must not be null");
    if (identifier == null || identifier.isBlank()) {
      return false;
    }
    AuthAttemptCounter.Counter state = counter.peek(scope.name(), attemptKey(scope, identifier));
    return state.blockedUntil() != null && now.isBefore(state.blockedUntil());
  }

  /**
   * Clears the bucket after a successful authentication.
   *
   * <p>Only ever called for a success. Clearing on failure would make the throttle a no-op.</p>
   *
   * @param scope      the flow
   * @param identifier the address as typed
   */
  public void onSuccess(Scope scope, String identifier) {
    requireNonNull(scope, "scope must not be null");
    if (identifier == null || identifier.isBlank()) {
      return;
    }
    counter.clear(scope.name(), attemptKey(scope, identifier));
  }

  private Policy policyFor(Scope scope) {
    return switch (scope) {
      case LOGIN -> loginPolicy;
      case EMAIL_CONFIRMATION_RESEND, PASSWORD_RESET_RESEND -> resendPolicy;
    };
  }

  /**
   * Derives the stored key: SHA-256 over {@code scope NUL normalised-identifier}.
   *
   * <p>Plain SHA-256, not a stretching function. The input is an email address — low entropy, so a
   * hash is not a secret — but it must not be <em>readable</em> in the table, because reading it
   * would turn a rate-limiting table into a list of addresses under attack. The scope is included so
   * one bucket cannot be replayed across flows.</p>
   */
  static String attemptKey(Scope scope, String identifier) {
    return sha256Hex(scope.name() + "\u0000" + normalise(identifier));
  }

  /**
   * Trims and lower-cases with {@link Locale#ROOT}.
   *
   * <p>{@code Locale.ROOT} is not pedantry: {@code "I".toLowerCase()} yields a dotless {@code ı} in
   * a Turkish locale, which would silently split the bucket for any address containing an I.</p>
   *
   * <p>{@code EmailAddress} performs no case folding or stripping, so without this
   * {@code Alice@x.de}, {@code alice@x.de} and {@code alice@x.de } are three buckets pointing at one
   * account — and varying capitalisation grants unlimited attempts per victim.</p>
   */
  static String normalise(String identifier) {
    return identifier.strip().toLowerCase(Locale.ROOT);
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandatory in every JRE. Failing closed here would be safer than continuing
      // without throttling, but there is no way to recover, so this is unreachable-by-contract.
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  /** Validated thresholds for one class of flow. */
  private record Policy(int maxAttempts, Duration window, Duration blockDuration, String name) {

    Policy {
      if (maxAttempts <= 0) {
        throw new IllegalArgumentException(
            "auth-throttle %s max-attempts must be positive, but was %d".formatted(name, maxAttempts));
      }
      requirePositive(window, name + " window");
      requirePositive(blockDuration, name + " block-duration");
    }

    private static void requirePositive(Duration duration, String what) {
      requireNonNull(duration, what + " must not be null");
      if (duration.isZero() || duration.isNegative()) {
        throw new IllegalArgumentException(what + " must be positive, but was " + duration);
      }
    }
  }
}
