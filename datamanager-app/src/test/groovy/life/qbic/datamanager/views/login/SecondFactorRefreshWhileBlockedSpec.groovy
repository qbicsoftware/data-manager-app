package life.qbic.datamanager.views.login

import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Regression: refreshing a blocked challenge must not render a usable form (QA report)</b>
 *
 * <p>Reported from the deployed build: after the challenge refused with "Too many attempts", a page
 * refresh brought back the normal second-factor window — code field live, Verify live — so the user
 * tried again, was refused again, and could not tell whether anything had changed.</p>
 *
 * <p>The block itself was never in doubt: it is anchored when the threshold is crossed and a retry
 * cannot move it. What was wrong is that the <em>page</em> only knew it was blocked after a
 * submission, because {@code beforeEnter} checked the pending login and nothing else. The user was
 * therefore told, once, and then shown a form that suggested the message had expired.</p>
 *
 * <p>Two properties make the fix honest, and both are asserted here rather than in the view:</p>
 * <ol>
 *   <li>Entering the page asks the throttle, so the blocked state is rendered on load.</li>
 *   <li>Asking is a read. Rendering must never record an attempt, or a reloading user would extend
 *       their own block — the opposite of what this whole change is for.</li>
 * </ol>
 */
class SecondFactorRefreshWhileBlockedSpec extends Specification {

  static final String USER = "u-1"
  static final Duration WINDOW = Duration.ofMinutes(15)
  static final Duration BLOCK = Duration.ofMinutes(15)
  static final int LIMIT = 3

  FakeCounter counter = new FakeCounter()
  AuthAttemptThrottle throttle = new AuthAttemptThrottle(counter,
      10, WINDOW, WINDOW,
      5, WINDOW, WINDOW,
      LIMIT, WINDOW, BLOCK)

  Instant now = Instant.parse("2026-03-01T10:00:00Z")

  // ---------------------------------------------------------------- what the page asks on entry

  def "entering the page during a block yields a wait, so the view can render the blocked state"() {
    given: "the account is blocked"
    (LIMIT + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }

    expect: "the view has something to show on load, not only after a submission"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now).isPresent()
  }

  def "entering the page when the block has run out yields nothing, so the form renders normally"() {
    given:
    (LIMIT + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }

    expect: "the user is not stuck on a blocked page after the wait"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER,
            now.plus(BLOCK)).isEmpty()
  }

  def "refreshing repeatedly does not extend the block"() {
    given:
    (LIMIT + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }
    def blockEnd = counter.blockedUntilFor(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER)

    when: "a user reloads the page ten times in a minute"
    10.times { throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }

    then: "the block still ends when it originally did"
    counter.blockedUntilFor(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER) == blockEnd
  }

  def "the remaining time shown after a refresh is less than after the refusal"() {
    given: "the refusal happened five minutes ago"
    (LIMIT + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }

    when:
    def atRefusal = throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now)
    def afterRefresh = throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER,
        now.plus(Duration.ofMinutes(5)))

    then: "the page stops overstating the wait, which is what made the message look stale"
    atRefusal.orElseThrow() == BLOCK
    afterRefresh.orElseThrow() == Duration.ofMinutes(10)
  }

  def "the sentence rendered for a refreshed page states the time actually left"() {
    given: "ten minutes remain"
    (LIMIT + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }
    def remaining = throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER,
        now.plus(Duration.ofMinutes(5))).orElseThrow()

    expect:
    SecondFactorView.throttledMessage(remaining)
        == "Too many failed attempts. You can try again in 10 minutes."
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

    Instant blockedUntilFor(AuthAttemptThrottle.Scope scope, String identifier) {
      def existing = state[bucket(scope.name(), AuthAttemptThrottle.attemptKey(scope, identifier))]
      return existing == null ? null : existing.blockedUntil()
    }

    record Row(int attemptCount, Instant windowStartedAt, Instant blockedUntil) {
    }
  }
}
