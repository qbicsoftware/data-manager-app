package life.qbic.identity.application.mfa

import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder
import life.qbic.identity.domain.repository.AuthAttemptCounter
import life.qbic.identity.domain.repository.RecoveryCodeRepository
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Regression: guessing recovery codes at the challenge must be throttled too (MFA-68 follow-up)</b>
 *
 * <p>The first version of the MFA-68 fix guarded {@code SecondFactorVerificationService.verify} and
 * left redemption alone. That put the limit on the wrong secret. A TOTP code is six digits that rotate
 * every thirty seconds; a recovery code is twelve characters from a 32-symbol alphabet — about 2^60 —
 * and it <em>stays valid</em> until it is redeemed. So the long-lived, static credential was the
 * unguarded one, and an attacker holding the password could sit on the challenge page and submit guesses
 * at whatever rate the server allowed.</p>
 *
 * <p>The two inputs share one budget on purpose. Two buckets would let an attacker spend five TOTP
 * attempts and then five recovery attempts in the same window against the same login; the question the
 * throttle answers is "how many times may this account try to get past the factor", not "how many
 * times per input field".</p>
 *
 * <p>Guessing is not the realistic attack at 2^60; burning a victim's codes with a scripted loop is,
 * and it has the same remedy.</p>
 */
class RecoveryCodeThrottleSpec extends Specification {

  static final String USER_ID = "u-1"
  static final String OTHER_USER_ID = "u-2"
  static final String PEPPER = "0123456789abcdef0123456789abcdef-deployment-pepper"
  static final int LIMIT = 3
  static final Duration BLOCK = Duration.ofMinutes(15)

  // The same in-memory store the neighbouring spec uses: unique hashes, use is final.
  RecoveryCodeServiceSpec.FakeRepository repository = new RecoveryCodeServiceSpec.FakeRepository()
  RecoveryCodeEncoder encoder = new RecoveryCodeEncoder(PEPPER)
  InMemoryCounter counter = new InMemoryCounter()
  AuthAttemptThrottle throttle = new AuthAttemptThrottle(counter,
      10, BLOCK, BLOCK,
      5, BLOCK, BLOCK,
      LIMIT, BLOCK, BLOCK)
  RecoveryCodeService service = new RecoveryCodeService(repository, encoder, throttle)

  Instant now = Instant.now()

  // ---------------------------------------------------------------- the gap

  def "a burst of wrong recovery codes is refused once the limit is reached"() {
    given:
    def attempts = (1..10).collect { submit(USER_ID, "AAAAAAAAAAAA".take(12), now) }

    expect: "only the configured number reached redemption"
    attempts.count { it.reachedRedemption() } == LIMIT

    and: "the rest were refused by the throttle, before the code was even hashed against the store"
    attempts.count { it.throttled() } == 10 - LIMIT

    and: "no guess was accepted"
    attempts.every { !it.redeemed() }
  }

  def "the budget is shared with the authenticator code, not counted separately"() {
    given: "the account has already spent its allowance at the challenge"
    LIMIT.times { service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now) }

    when: "the same account then offers a recovery code"
    def outcome = service.redeemFor(USER_ID, "BBBBBBBBBBBB".take(12), now.plusSeconds(1))

    then: "it is refused, so switching input field does not buy fresh attempts"
    outcome.throttled()
    !outcome.redeemed()
  }

  def "one account's failures do not throttle another account"() {
    given:
    (LIMIT + 1).times { service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now) }

    when:
    def other = service.redeemFor(OTHER_USER_ID, "AAAAAAAAAAAA".take(12), now)

    then:
    !other.throttled()
  }

  def "the throttle lifts after the block duration"() {
    given:
    (LIMIT + 1).times { service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now) }

    when:
    def later = service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now.plus(BLOCK).plusSeconds(1))

    then:
    !later.throttled()
  }

  // ---------------------------------------------------------------- the real user

  def "a valid code is accepted and clears the allowance"() {
    given: "the user mistyped twice, one short of the limit"
    def set = service.generate(USER_ID, now)
    (LIMIT - 1).times { service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now) }

    when: "the right code finally goes in"
    def accepted = service.redeemFor(USER_ID, set.codes()[0], now)

    then:
    accepted.redeemed()

    and: "the allowance starts over, so the next typo is not the fourth attempt of an old window"
    def afterSuccess = (1..2).collect {
      service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now)
    }
    afterSuccess.every { !it.throttled() }
  }

  // ---------------------------------------------------------------- the notification

  def "a throttled refusal tells the caller how long to wait"() {
    given:
    (LIMIT + 1).times { service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now) }

    when:
    def outcome = service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now)

    then: "the view has something honest to say, instead of the 'wrong code' sentence forever"
    outcome.throttled()
    outcome.retryAfter() == BLOCK
  }

  def "a refusal for a wrong code is not a throttle refusal"() {
    expect: "the two are distinguishable, so only one of them is described as a rate limit"
    def outcome = service.redeemFor(USER_ID, "AAAAAAAAAAAA".take(12), now)
    !outcome.throttled()
    !outcome.redeemed()
    outcome.outcome() == RecoveryCodeService.Outcome.REJECTED
  }

  // ---------------------------------------------------------------- helpers

  private Outcome submit(String userId, String code, Instant at) {
    return new Outcome(service.redeemFor(userId, code, at))
  }

  record Outcome(RecoveryCodeService.Redemption redemption) {
    boolean throttled() {
      return redemption.throttled()
    }

    boolean redeemed() {
      return redemption.redeemed()
    }

    boolean reachedRedemption() {
      return !redemption.throttled()
    }
  }

  /** Mirrors the SQL adapter: the window roll-over clears the block. */
  static class InMemoryCounter implements AuthAttemptCounter {
    final Map<String, Row> state = [:]

    private static String bucket(String scope, String identifier) {
      return scope + "|" + identifier
    }

    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      def id = bucket(scope, key)
      def existing = state[id]
      boolean stale = existing == null || !now.isBefore(existing.windowStartedAt().plus(window))
      def updated = stale
          ? new Row(1, now, null)
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
