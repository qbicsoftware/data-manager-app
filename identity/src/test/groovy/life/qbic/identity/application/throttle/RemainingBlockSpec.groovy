package life.qbic.identity.application.throttle

import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Reading the remaining block must never record an attempt (refresh-safety)</b>
 *
 * <p>The challenge page has to render its blocked state on load, not only after a submission. That
 * means asking the throttle during a page render — and a render is not an attempt. If the read path
 * recorded anything, every refresh would push the block further out, and a user who reloads the page
 * (the natural reaction to "you are blocked") would punish themselves indefinitely.</p>
 *
 * <p>These cases pin the property by measuring, not by inspecting: the remaining block must be
 * monotonic non-increasing across repeated reads, and unchanged by reads at all. A retry during a
 * live block must also leave it exactly as it was — that is the combined guarantee that makes the
 * rendered countdown trustworthy.</p>
 */
class RemainingBlockSpec extends Specification {

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

  // ---------------------------------------------------------------- unblocked

  def "an unblocked account reports no remaining block"() {
    expect:
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now).isEmpty()
  }

  def "an account below the limit reports no remaining block"() {
    given:
    (LIMIT - 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }

    expect:
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now).isEmpty()
  }

  // ---------------------------------------------------------------- blocked

  def "a blocked account reports how long is left"() {
    given: "the limit is crossed one second into the window"
    def crossedAt = now.plusSeconds(1)
    (LIMIT + 1).times {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, crossedAt)
    }

    expect: "the block runs from the crossing, so this is the full duration, minus elapsed time"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, crossedAt)
            .orElseThrow() == BLOCK

    and: "five minutes later, ten are left"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER,
            crossedAt.plus(Duration.ofMinutes(5))).orElseThrow() == Duration.ofMinutes(10)
  }

  def "reading the remaining block neither extends nor shortens it"() {
    given:
    def crossedAt = now
    (LIMIT + 1).times {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, crossedAt)
    }
    def before = throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, crossedAt)

    when: "the page is rendered fifty times, as a refreshing user would"
    50.times { throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, crossedAt) }

    then: "nothing changed — a render is not an attempt"
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, crossedAt) == before

    and: "the counter recorded no extra attempts either"
    counter.attemptsFor(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER) == LIMIT + 1
  }

  def "the rendered state does not depend on how often the page was rendered"() {
    given:
    (LIMIT + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }

    expect: "the same read, however many times it happens, gives the same answer"
    def first = throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now)
    (1..10).every { throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) == first }
  }

  def "retrying during a live block does not extend it"() {
    given:
    def crossedAt = now
    (LIMIT + 1).times {
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, crossedAt)
    }
    def initialBlockEnd = counter.blockedUntilFor(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER)

    when: "the user hammers the verify button for two minutes"
    20.times { offset ->
      throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER,
          crossedAt.plusSeconds(offset * 6L))
    }

    then: "the block still ends when it originally did — the attacker cannot self-extend"
    counter.blockedUntilFor(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER) == initialBlockEnd
  }

  def "the remaining block never grows as time passes"() {
    given:
    (LIMIT + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }

    when: "the page is rendered once a minute, up to and including the minute the block ends"
    def samples = (0..15).collect { minute ->
      throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER,
          now.plus(Duration.ofMinutes(minute)))
    }

    then: "monotonically non-increasing, one minute less each time while the block lasts"
    samples.first().orElseThrow() == BLOCK
    def whileBlocked = samples.findAll { it.isPresent() }
    (1..<whileBlocked.size()).every { index ->
      whileBlocked[index].orElseThrow() <= whileBlocked[index - 1].orElseThrow()
    }

    and: "nothing left to report once the block has run out"
    samples.last().isEmpty()
  }

  def "once the block has run out there is nothing left to report"() {
    given:
    (LIMIT + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER, now) }

    expect:
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, USER,
            now.plus(BLOCK)).isEmpty()
  }

  def "a blank identifier has no block to report"() {
    expect:
    throttle.remainingBlock(AuthAttemptThrottle.Scope.SECOND_FACTOR, "  ", now).isEmpty()
  }

  /**
   * A counter that models the SQL upsert, because the block semantics live there: a row is reset (and
   * its block cleared) only when the window has elapsed, and a live block is left untouched.
   */
  static class FakeCounter implements AuthAttemptCounter {
    final Map<String, Row> state = [:]

    private static String bucket(String scope, String identifier) {
      return scope + "|" + identifier
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

    /** The throttle keys rows by the hashed attempt key, not the raw identifier. */
    int attemptsFor(AuthAttemptThrottle.Scope scope, String identifier) {
      def existing = state[bucket(scope.name(), AuthAttemptThrottle.attemptKey(scope, identifier))]
      return existing == null ? 0 : existing.attemptCount()
    }

    Instant blockedUntilFor(AuthAttemptThrottle.Scope scope, String identifier) {
      def existing = state[bucket(scope.name(), AuthAttemptThrottle.attemptKey(scope, identifier))]
      return existing == null ? null : existing.blockedUntil()
    }

    record Row(int attemptCount, Instant windowStartedAt, Instant blockedUntil) {
    }
  }
}
