package life.qbic.projectmanagement.infrastructure.sample.openbis;

import java.time.Duration;
import java.util.Random;
import java.util.function.LongSupplier;

/**
 * <b>Retry policy for openBIS operations</b>
 *
 * <p>Bounds how long a single openBIS operation may take, so that a server-side retry can never
 * outlive the UI timeout waiting for it.
 *
 * <p>The budget is a <em>wall-clock deadline</em>, not a sum of sleeps. That distinction matters:
 * each attempt is an HTTP call with its own read timeout ({@code
 * openbis.datasource.read-timeout-ms}), so an unresponsive server can spend that timeout on every
 * attempt. Bounding only the sleeps leaves the worst case at {@code attempts * readTimeout +
 * sleeps} — several times the number the policy advertises.
 *
 * <p>See issue #1700. Before this policy the retry loop used {@code 500 * 2^round} for rounds 1-9
 * with no cap (about 511 s of sleeping) behind a 300 s UI timeout, so the UI could report a
 * failure for an operation that was still running and could still commit.
 *
 * <p>This policy bounds <em>timing</em> only. Whether a failure is worth retrying at all is a
 * separate concern: permanent errors are still retried, which is tracked by #1699.
 *
 * @since 1.20.3
 */
public final class OpenbisRetryPolicy {

  /**
   * Maximum number of execution attempts, including the first one.
   */
  public static final int MAX_ATTEMPTS = 10;

  /**
   * Initial backoff before the second attempt.
   */
  public static final Duration INITIAL_BACKOFF = Duration.ofMillis(500);

  /**
   * Upper bound for the jitter added to each backoff, to avoid retry storms between nodes.
   */
  public static final Duration MAX_JITTER = Duration.ofMillis(200);

  /**
   * Upper bound for any single backoff, so the exponential growth stops early.
   */
  public static final Duration MAX_BACKOFF = Duration.ofSeconds(8);

  /**
   * Wall-clock budget for a single operation, covering the calls themselves, the backoffs and the
   * time spent unwinding failures. No attempt is started once the deadline has passed.
   *
   * <p>Sixty seconds is deliberately short: a user is waiting, and an operation that has not
   * succeeded after a minute of retrying is better reported as failed. It also keeps the worst case
   * for a request (see {@link #MAX_OPERATIONS_PER_REQUEST}) inside the derived UI timeout.
   */
  public static final Duration MAX_OPERATION_DURATION = Duration.ofSeconds(60);

  /**
   * Documented upper bound for the number of openBIS operations a single user request performs.
   * Used by the views to derive their timeout from this policy.
   */
  public static final int MAX_OPERATIONS_PER_REQUEST = 4;

  /**
   * Margin added on top of the worst case request duration when deriving a UI timeout.
   */
  public static final Duration UI_TIMEOUT_MARGIN = Duration.ofSeconds(20);

  private OpenbisRetryPolicy() {
    // static utility
  }

  /**
   * Returns whether another attempt may be started after {@code attempted} attempts.
   *
   * @param attempted the number of attempts already made
   */
  public static boolean hasAttemptsLeft(int attempted) {
    return attempted < MAX_ATTEMPTS;
  }

  /**
   * Returns whether an attempt may start, given the time left before the deadline.
   *
   * @param remainingMillis wall-clock time left for this operation
   */
  public static boolean withinBudget(long remainingMillis) {
    return remainingMillis > 0;
  }

  /**
   * Calculates the jittered, capped backoff before the given attempt.
   *
   * @param nextAttempt the 1-based number of the attempt that follows the wait
   * @param random      the source of jitter, injectable for tests
   * @return the time to sleep in milliseconds
   */
  public static long backoffMillis(int nextAttempt, Random random) {
    if (nextAttempt < 1) {
      throw new IllegalArgumentException("nextAttempt must be >= 1");
    }
    long exponential = INITIAL_BACKOFF.toMillis() << (nextAttempt - 1);
    long jitter = random.nextInt((int) MAX_JITTER.toMillis() + 1);
    return Math.min(exponential + jitter, MAX_BACKOFF.toMillis());
  }

  /**
   * Returns whether waiting {@code nextBackoffMillis} still fits before the deadline.
   *
   * @param remainingMillis   wall-clock time left for this operation
   * @param nextBackoffMillis the backoff that would be applied next
   */
  public static boolean fitsBudget(long remainingMillis, long nextBackoffMillis) {
    return remainingMillis - nextBackoffMillis > 0;
  }

  /**
   * The worst case wall-clock time for a single user request: every operation running to its full
   * budget, plus one in-flight call that is abandoned when the deadline passes.
   *
   * @param readTimeout the configured HTTP read timeout for a single openBIS call
   */
  public static Duration worstCaseRequestDuration(Duration readTimeout) {
    Duration perOperation = MAX_OPERATION_DURATION.plus(readTimeout);
    return perOperation.multipliedBy(MAX_OPERATIONS_PER_REQUEST);
  }

  /**
   * The timeout a view must use when awaiting a request that performs openBIS operations.
   *
   * <p>Derived from the worst case plus a margin, so the server always finishes before the UI gives
   * up. Deriving it here rather than hardcoding it in a view is what keeps the two from drifting
   * apart: the previous arrangement allowed a roughly 511 s retry loop behind a 300 s timeout, so
   * the UI could report failure for an operation that later committed.
   *
   * @param readTimeout the configured HTTP read timeout for a single openBIS call
   */
  public static Duration recommendedUiTimeout(Duration readTimeout) {
    return worstCaseRequestDuration(readTimeout).plus(UI_TIMEOUT_MARGIN);
  }

  /**
   * Runs {@code attempt} until it succeeds, the attempts are exhausted, or the deadline passes.
   *
   * <p>The loop is extracted here so it can be tested without a live openBIS server: both the
   * original defect and its first (incomplete) fix lived in this loop, and neither was covered by a
   * test.
   *
   * @param attempt   the operation to run; signalling failure by throwing
   * @param random    the source of jitter
   * @param nanoClock the monotonic clock, injectable so tests need not sleep
   * @param sleeper   the sleep function, injectable so tests need not sleep
   * @param onFailure called with every failed attempt and the number of attempts left
   * @throws RetriesExhaustedException when no attempt succeeded
   */
  public static void execute(Runnable attempt, Random random, LongSupplier nanoClock,
      Sleeper sleeper, FailureListener onFailure) {
    long deadline = nanoClock.getAsLong() + MAX_OPERATION_DURATION.toNanos();
    Exception lastFailure = null;
    for (int attemptNumber = 1; hasAttemptsLeft(attemptNumber - 1); attemptNumber++) {
      try {
        attempt.run();
        return;
      } catch (RuntimeException e) {
        lastFailure = e;
        onFailure.onFailure(e, attemptNumber);
      }
      long remainingMillis =
          Math.max(0, (deadline - nanoClock.getAsLong()) / 1_000_000L);
      if (!hasAttemptsLeft(attemptNumber) || !withinBudget(remainingMillis)) {
        throw new RetriesExhaustedException(lastFailure);
      }
      long backoff = backoffMillis(attemptNumber, random);
      if (!fitsBudget(remainingMillis, backoff)) {
        throw new RetriesExhaustedException(lastFailure);
      }
      sleeper.sleep(backoff);
    }
    throw new RetriesExhaustedException(lastFailure);
  }

  /**
   * Sleeping, injectable so tests do not actually wait.
   */
  @FunctionalInterface
  public interface Sleeper {

    void sleep(long millis);
  }

  /**
   * Notification for every failed attempt, used for logging.
   */
  @FunctionalInterface
  public interface FailureListener {

    void onFailure(RuntimeException failure, int attemptNumber);
  }

  /**
   * Signals that no attempt succeeded within the retry budget. Carries the last failure as its
   * cause, so callers can distinguish a duplicate code from a network outage.
   */
  public static class RetriesExhaustedException extends RuntimeException {

    public RetriesExhaustedException(Throwable cause) {
      super("Unexpected exception during openBIS operation.", cause);
    }
  }
}
