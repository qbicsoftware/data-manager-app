package life.qbic.datamanager.views.login

import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Regression: one account's block must not disable the challenge for another account</b>
 *
 * <p>Found on a deployed build right after the refresh fix: account A exhausted its attempts, the
 * page rendered its blocked state, and the visitor then chose "Use a different account". They signed
 * in as B — a different account, not blocked, nothing to do with A — and arrived at a challenge whose
 * fields were <em>still disabled</em>, with A's notice at the bottom. B could do nothing but wait out
 * A's fifteen minutes.</p>
 *
 * <h3>Why "it is the same client" does not make this correct</h3>
 * <p>Same browser, yes. But the second-factor bucket is keyed on the <b>account</b>, deliberately and
 * documented as such: reaching the flow requires a verified password for a specific account, so the
 * budget belongs to that account. Punishing B for A's guesses is not a rate limit, it is a lockout of
 * an uninvolved user — and it is the same failure mode the throttle's own class documentation
 * forbids in Rule 3 ("per-account-only throttling hands an attacker a denial of service against a
 * chosen victim"). Anybody who can type A's wrong password ten times from a shared machine at a
 * conference or in a shared lab would lock that browser out of every account — B's included.</p>
 *
 * <h3>The actual defect, which is in the view</h3>
 * <p>{@code SecondFactorView} is {@code @UIScope}: one instance per UI, reused across navigations.
 * The blocked state disables the components and <em>nothing re-enables them</em>, so the disabling
 * outlived the account it belonged to. The throttle was right all along — B's bucket is empty — the
 * page simply never asked again.</p>
 *
 * <p>Note the ordering this pins: the state must be decided from the current pending login on every
 * entry, and it must be a full decision (enable or disable), not a one-way one.</p>
 */
class SecondFactorSwitchWhileBlockedSpec extends Specification {

  static final String ACCOUNT_A = "u-a"
  static final String ACCOUNT_B = "u-b"
  static final Duration WINDOW = Duration.ofMinutes(15)
  static final Duration BLOCK = Duration.ofMinutes(15)
  static final int LIMIT = 3

  FakeCounter counter = new FakeCounter()
  AuthAttemptThrottle throttle = new AuthAttemptThrottle(counter,
      10, WINDOW, WINDOW,
      5, WINDOW, WINDOW,
      LIMIT, WINDOW, BLOCK)

  Instant now = Instant.parse("2026-03-01T10:00:00Z")

  // ---------------------------------------------------------------- the report

  def "A's block does not follow the visitor to B"() {
    given: "A exhausts its allowance"
    (LIMIT + 1).times {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_A, now)
    }

    expect: "the buckets are per account, so B is untouched"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_A, now).isPresent()
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_B, now).isEmpty()
  }

  def "the second factor has no origin bucket to explain a shared block"() {
    given: "A is blocked and the same client submits as B, many times over"
    (LIMIT + 1).times {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_A, now)
    }

    when: "B submits wrong codes up to its own limit"
    def outcomes = (1..LIMIT).collect {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_B, now)
    }

    then: "every one of B's own attempts was allowed: no origin bucket exists for this flow"
    outcomes.every { it == AuthAttemptThrottle.Decision.ALLOWED }

    and: "and B still has no block until B itself crosses the limit"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_B, now).isEmpty()
  }

  def "B is blocked only after B's own attempts, on B's own budget"() {
    given: "A is blocked"
    (LIMIT + 1).times {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_A, now)
    }

    when: "B crosses its own limit"
    (LIMIT + 1).times {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_B, now)
    }

    then: "B now has its own block, independent of A's"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_B, now).isPresent()

    and: "A's block is unaffected by anything B did"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_A, now)
            .orElseThrow() == BLOCK
  }

  def "B is admitted for the whole of A's block"() {
    given:
    (LIMIT + 1).times {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_A, now)
    }

    expect: "at no point during A's block is B refused"
    (0..14).every { minute ->
      throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, ACCOUNT_B,
          now.plus(Duration.ofMinutes(minute))).isEmpty()
    }
  }

  // ---------------------------------------------------------------- helpers

  static class FakeCounter implements AuthAttemptCounter {
    final Map<String, Row> state = [:]

    private static String bucket(String scope, String key) {
      return scope + "|" + key
    }

    @Override
    Counter increment(String scope, String key, Instant at, Duration window) {
      def id = bucket(scope, key)
      def existing = state[id]
      boolean stale = existing == null || !at.isBefore(existing.windowStartedAt().plus(window))
      def updated = stale
          ? new Row(1, at, null)
          : new Row(existing.attemptCount() + 1, existing.windowStartedAt(),
              existing.blockedUntil())
      state[id] = updated
      return new Counter(updated.attemptCount(), updated.blockedUntil())
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
      def id = bucket(scope, key)
      def existing = state[id]
      state[id] = new Row(existing == null ? 0 : existing.attemptCount(),
          existing == null ? Instant.EPOCH : existing.windowStartedAt(), blockedUntil)
    }

    @Override
    Counter peek(String scope, String key) {
      def existing = state[bucket(scope, key)]
      return existing == null ? new Counter(0, null)
          : new Counter(existing.attemptCount(), existing.blockedUntil())
    }

    @Override
    void clear(String scope, String key) {
      state.remove(bucket(scope, key))
    }

    record Row(int attemptCount, Instant windowStartedAt, Instant blockedUntil) {
    }
  }
}
