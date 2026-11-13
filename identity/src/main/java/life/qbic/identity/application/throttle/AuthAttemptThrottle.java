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
import org.springframework.transaction.annotation.Transactional;

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
 * <h3>Rule 5 — every write runs inside a transaction, and this class is what provides it</h3>
 * <p>The counter uses native SQL on a container-managed {@code EntityManager}; without an active
 * transaction an {@code INSERT}/{@code UPDATE} throws, and because the login counter is written from
 * a Spring authentication <em>event listener</em>, that exception escapes through the event publisher
 * and surfaces as an HTTP 500 — after the credentials have already been rejected.</p>
 *
 * <p>{@code @Transactional} is therefore on the public methods here, not on the repository. The
 * repository must stay transaction-agnostic: the token-consumption adapter deliberately joins the
 * caller's transaction so consumption and the state change it authorises commit together, and the
 * same rule applies here.</p>
 *
 * <p>This was a genuine production bug that the concurrency specs could not catch: every one of them
 * wrapped the counter in its own transaction, so they supplied the boundary that production never
 * opened.</p>
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
    PASSWORD_RESET_RESEND,
    /**
     * Submissions of the registration form.
     *
     * <p>Added with FEAT-AUTH-FLOWS-03 because the unauthenticated registration form is a request
     * surface like the others: without a throttle, an attacker can mass-submit addresses to make the
     * application send notification mail, and can repeatedly invalidate a victim's outstanding reset
     * link, since each newly issued token supersedes the previous one.</p>
     */
    REGISTRATION
  }

  /** Whether the caller may perform the work. Never render this; see Rule 2. */
  public enum Decision {
    ALLOWED,
    THROTTLED
  }

  /**
   * The outcome of one attempt.
   *
   * <p>Carries the decision plus what a view needs to describe the state honestly:</p>
   * <ul>
   *   <li>{@link #attemptsUsed()} — how many attempts are counted in the current window;</li>
   *   <li>{@link #attemptsRemainingInWindow()} — how many more will succeed before the block.</li>
   * </ul>
   *
   * <p>Both describe <em>configured policy</em> and the caller's own attempt count, never whether an
   * account exists. A view may render them on a page the visitor has already reached legitimately
   * (e.g. after proving they know the user id), but must NOT render them where the audience is
   * anonymous — see {@code ResetPasswordMain} for that case.</p>
   *
   * @param decision    whether the work may proceed
   * @param attemptsUsed attempts counted in the window after this one
   * @param maxAttempts  the configured limit for the flow
   */
  public record Outcome(Decision decision, int attemptsUsed, int maxAttempts) {

    /** @return how many further attempts will still be allowed in this window */
    public int attemptsRemainingInWindow() {
      return Math.max(0, maxAttempts - attemptsUsed);
    }

    public boolean isAllowed() {
      return decision == Decision.ALLOWED;
    }
  }

  /**
   * Records an attempt and reports the outcome.
   *
   * <p>Preferred over {@link #record(Scope, String, Instant)}, which loses the counts callers need to
   * tell a visitor how many attempts remain.</p>
   */
  @Transactional
  public Outcome recordAndReport(Scope scope, String identifier, Instant now) {
    requireNonNull(scope, "scope must not be null");
    requireNonNull(now, "now must not be null");
    if (identifier == null || identifier.isBlank()) {
      return new Outcome(Decision.ALLOWED, 0, maxAttempts(scope));
    }

    Policy policy = policyFor(scope);
    String key = attemptKey(scope, identifier);
    AuthAttemptCounter.Counter state = counter.increment(scope.name(), key, now, policy.window());

    if (state.blockedUntil() != null && now.isBefore(state.blockedUntil())) {
      return new Outcome(Decision.THROTTLED, state.attemptCount(), policy.maxAttempts());
    }
    if (state.attemptCount() > policy.maxAttempts()) {
      counter.block(scope.name(), key, now.plus(policy.blockDuration()));
      log.info("Throttling %s attempts after %d attempts in the window"
          .formatted(scope, state.attemptCount()));
      return new Outcome(Decision.THROTTLED, state.attemptCount(), policy.maxAttempts());
    }
    return new Outcome(Decision.ALLOWED, state.attemptCount(), policy.maxAttempts());
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
   * <p>{@code @Transactional} is not decoration: without it the native upsert throws because there is
   * no active transaction, and the login path surfaces that as a 500. See Rule 5 in the class
   * javadoc.</p>
   *
   * @param scope the flow
   * @param identifier the address as typed; normalised and hashed here, never stored raw
   * @param now   the attempt instant
   * @return whether to perform the work. <b>Do not render this.</b>
   */
  @Transactional
  public Decision record(Scope scope, String identifier, Instant now) {
    return recordAndReport(scope, identifier, now).decision();
  }

  /**
   * Reports whether this identifier is currently blocked, <b>without recording an attempt</b>.
   *
   * <p>Called before authentication is attempted. The result is for control flow only — the caller
   * must produce the <em>same</em> generic failure it would have produced anyway (see Rule 2).</p>
   *
   * <p>Read-only, but still needs a transaction: the native query runs on the container-managed
   * {@code EntityManager}.</p>
   *
   * @param scope      the flow
   * @param identifier the address as typed
   * @param now        the instant to evaluate against
   * @return {@code true} if the caller should skip the work
   */
  @Transactional(readOnly = true)
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
  @Transactional
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
      case EMAIL_CONFIRMATION_RESEND, PASSWORD_RESET_RESEND, REGISTRATION -> resendPolicy;
    };
  }

  /**
   * How long a block lasts for this flow.
   *
   * <p>Exposed so a view can tell the visitor when to come back, instead of leaving them to guess
   * whether anything happened. It reveals a configured duration, not account state.</p>
   *
   * @param scope the flow
   * @return the configured block duration
   */
  public Duration blockDuration(Scope scope) {
    return policyFor(requireNonNull(scope, "scope must not be null")).blockDuration();
  }

  /**
   * How many attempts are permitted in one window for this flow.
   *
   * <p>Exposed so a view can say "you can request N more" instead of leaving the visitor to discover
   * the limit by hitting it. This is configured policy, not account state.</p>
   *
   * <p>Semantics: exactly {@code maxAttempts} attempts succeed per window. {@link #record} compares the
   * count <em>after</em> the increment against this value with a strict {@code >}, so the
   * {@code maxAttempts + 1}-th attempt is the first one refused.</p>
   *
   * @param scope the flow
   * @return the configured maximum attempts per window
   */
  public int maxAttempts(Scope scope) {
    return policyFor(requireNonNull(scope, "scope must not be null")).maxAttempts();
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
