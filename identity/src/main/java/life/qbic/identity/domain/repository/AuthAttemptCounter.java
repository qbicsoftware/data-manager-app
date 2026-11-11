package life.qbic.identity.domain.repository;

import java.time.Instant;

/**
 * <b>Attempt counter</b>
 *
 * <p>Persistence port behind {@link life.qbic.identity.application.throttle.AuthAttemptThrottle}.
 * Implemented in {@code identity-infrastructure} as a database upsert.</p>
 *
 * @since 1.0.0
 */
public interface AuthAttemptCounter {

  /**
   * Increments the counter for one (scope, key) in a single database statement, rolling the window
   * over when it has elapsed.
   *
   * <p><b>Why this is one statement and not read-then-write.</b> Two concurrent attempts both read
   * {@code attemptCount = 3} and both write {@code 4}, so an attacker gains one free attempt per
   * race. The increment must be performed by the database.</p>
   *
   * @param scope      the flow being throttled
   * @param key        opaque hashed key material; never a raw address
   * @param now        the attempt instant
   * @param windowStart attempts before this instant do not count towards the current window
   * @return the state <em>after</em> the increment
   */
  Counter increment(String scope, String key, Instant now, Instant windowStart);

  /**
   * Records that the counter is blocked until {@code blockedUntil}.
   *
   * <p>Called after the threshold is crossed. Kept separate from
   * {@link #increment(String, String, Instant, Instant)} because the decision is made in Java
   * against an {@link Instant}: writing a SQL-computed {@code blockedUntil} would bind the decision
   * to the database session's timezone, and every column here is {@code datetime(6)} with no zone.</p>
   *
   * @param scope        the flow
   * @param key          the key
   * @param blockedUntil when the block expires
   */
  void block(String scope, String key, Instant blockedUntil);

  /**
   * Reads the current state <b>without</b> incrementing.
   *
   * <p>Used by the pre-request check. It must not write: a request rejected by the throttle is not
   * an attempt, and counting it would let a blocked client inflate its own counter — and, worse,
   * silently extend its own block by retrying.</p>
   *
   * @param scope the flow
   * @param key   the key
   * @return the current state, with {@code attemptCount = 0} when no row exists
   */
  Counter peek(String scope, String key);

  /**
   * Clears the counter, called after a successful authentication.
   *
   * <p>Without this, a user who mistypes their password a few times and then succeeds stays one
   * attempt away from a block for the rest of the window.</p>
   *
   * @param scope the flow
   * @param key   the key
   */
  void clear(String scope, String key);

  /**
   * @param attemptCount the count after the increment
   * @param blockedUntil when the block expires, or {@code null} if the counter is not blocked
   */
  record Counter(int attemptCount, Instant blockedUntil) {

  }
}
