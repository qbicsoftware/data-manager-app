package life.qbic.identity.application.verification;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import life.qbic.identity.domain.model.verification.VerificationTokens;

/**
 * <b>Auth attempt throttle</b>
 *
 * <p>Per-account throttling for the unauthenticated flows (AUTH-NFR-03). Backed by the
 * {@code auth_attempt} table.</p>
 *
 * <h3>Why the key is a hash of the normalised address, and why there is no FK to {@code users}</h3>
 * <p>An attempt against an address that does not exist must be counted <em>identically</em> to an
 * attempt against one that does. If the table had a foreign key to {@code users}, the unknown
 * address could not be recorded at all, and the mere presence or absence of throttling would answer
 * "does this account exist?" — the enumeration oracle AUTH-NFR-03 forbids.</p>
 *
 * <h3>Why normalisation is not optional</h3>
 * <p>{@code EmailAddress} performs no case folding or trimming, so {@code Alice@x.de},
 * {@code alice@x.de} and {@code alice@x.de } would otherwise be three separate buckets pointing at
 * the same account — and an attacker could vary capitalisation to get unlimited reset mail per
 * victim. The same normalisation must be used by the login lookup, or the throttle and the
 * authentication disagree about which account is which.</p>
 *
 * <h3>Why the update is an upsert</h3>
 * <p>Read-then-write loses increments: two concurrent attempts both read {@code attemptCount = 3}
 * and both write {@code 4}, so an attacker gains a free attempt per race. The counter must be
 * incremented by the database in one statement.</p>
 *
 * @since 1.0.0
 */
public class AuthAttemptThrottle {

  /** The flows that are throttled. Each is an independent bucket. */
  public enum Scope {
    LOGIN,
    EMAIL_CONFIRMATION_RESEND,
    PASSWORD_RESET_RESEND
  }

  private final AttemptCounter counter;
  private final int maxAttempts;
  private final Duration window;
  private final Duration blockDuration;

  public AuthAttemptThrottle(AttemptCounter counter, int maxAttempts, Duration window,
      Duration blockDuration) {
    this.counter = requireNonNull(counter, "counter must not be null");
    if (maxAttempts <= 0) {
      throw new IllegalArgumentException("maxAttempts must be positive, but was " + maxAttempts);
    }
    this.maxAttempts = maxAttempts;
    this.window = requireNonNull(window, "window must not be null");
    this.blockDuration = requireNonNull(blockDuration, "blockDuration must not be null");
  }

  /**
   * Normalises an account identifier for keying.
   *
   * <p>Trim then lower-case, using {@link java.util.Locale#ROOT} so the result does not depend on
   * the server's default locale (the Turkish dotless-i problem: {@code "I".toLowerCase()} is
   * {@code "ı"} in a Turkish locale, which would silently split the bucket).</p>
   *
   * @param email the address as typed by the user
   * @return the normalised key material
   */
  static String normaliseAccountKey(String email) {
    requireNonNull(email, "email must not be null");
    return email.strip().toLowerCase(java.util.Locale.ROOT);
  }

  /**
   * @param scope the flow
   * @param email the address attempted against
   * @return the opaque key stored in {@code auth_attempt.attemptKey}
   */
  static String attemptKey(Scope scope, String email) {
    String material = scope.name() + "\u0000" + normaliseAccountKey(email);
    return VerificationTokens.hash(material);
  }

  /**
   * Records an attempt and reports whether the caller may proceed.
   *
   * @param scope the flow
   * @param email the address attempted against
   * @param now   the attempt instant
   * @return {@code true} if the attempt is allowed, {@code false} if the caller is blocked
   */
  public boolean recordAndCheck(Scope scope, String email, Instant now) {
    requireNonNull(scope, "scope must not be null");
    requireNonNull(now, "now must not be null");
    String key = attemptKey(scope, email);

    AttemptCounter.Counter state = counter.increment(scope, key, now);
    if (state.blockedUntil() != null && now.isBefore(state.blockedUntil())) {
      return false;
    }
    return state.attemptCount() <= maxAttempts;
  }

  /**
   * Clears the counter for an account, called after a successful authentication or a successfully
   * consumed link.
   *
   * <p>Without this, a user who fat-fingers their password 5 times and then succeeds stays one
   * attempt away from a block for the rest of the window.</p>
   */
  public void reset(Scope scope, String email, Instant now) {
    requireNonNull(scope, "scope must not be null");
    requireNonNull(now, "now must not be null");
    counter.clear(scope, attemptKey(scope, email));
  }

  public int maxAttempts() {
    return maxAttempts;
  }

  public Duration window() {
    return window;
  }

  public Duration blockDuration() {
    return blockDuration;
  }

  /**
   * Port for the counter storage. Implemented in {@code identity-infrastructure} as an upsert.
   */
  public interface AttemptCounter {

    /**
     * Increments the counter for (scope, key) in a single database statement.
     *
     * @return the state <em>after</em> the increment
     */
    Counter increment(Scope scope, String key, Instant now);

    void clear(Scope scope, String key);

    /**
     * @param attemptCount the count after increment
     * @param blockedUntil the block expiry, or {@code null} if not blocked
     */
    record Counter(int attemptCount, Instant blockedUntil) {

    }
  }
}
