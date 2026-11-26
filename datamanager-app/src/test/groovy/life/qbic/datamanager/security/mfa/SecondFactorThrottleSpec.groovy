package life.qbic.datamanager.security.mfa

import life.qbic.identity.application.mfa.SecondFactorSecretProtector
import life.qbic.identity.application.mfa.SecondFactorVerificationService
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.model.mfa.Totp
import life.qbic.identity.domain.repository.AuthAttemptCounter
import life.qbic.identity.domain.repository.SecondFactorRepository
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Regression: repeated failed second-factor codes must be throttled (MFA-68)</b>
 *
 * <p>The QA report is blunt and correct: "I spammed the mouse click for like 2 minutes and nothing
 * stopped me", with the reporter adding they were not sure the control existed at all. It did not.
 * {@code AuthAttemptThrottle} guards the password, the confirmation resend, the password-reset resend
 * and registration — the {@code Scope} enum's own documentation names "second-factor throttling
 * (MILESTONE-MFA M5-4)" as the intended next user of that mechanism — but nothing ever recorded or
 * consulted a second-factor attempt. The challenge page simply verified, refused, and let the visitor
 * try again, forever.</p>
 *
 * <p>Why this is not a nice-to-have: a six-digit code is one guess in a million per thirty-second
 * step, which is safe only while an attacker gets <em>one</em> guess per step. Without a throttle the
 * same million guesses are a few minutes of scripted submissions against the challenge, and the
 * account's second factor is worth no more than its password.</p>
 *
 * <h3>What the throttle must and must not do</h3>
 * <ul>
 *   <li><b>Key on the pending account, not an address.</b> The password step already established
 *       which account is mid-login, and one account's failures must not spend another's allowance.</li>
 *   <li><b>Refuse the attempt, not the session.</b> A throttled submission must leave the pending
 *       login alone: locking the user out of the challenge page would turn a rate limit into the
 *       lockout it exists to prevent.</li>
 *   <li><b>Clear on success</b>, so a user who mistyped twice and then got it right starts fresh.</li>
 *   <li><b>Say nothing different.</b> The refusal renders the same sentence as a wrong code
 *       (AUTH-NFR-03); the distinction only helps whoever is guessing.</li>
 * </ul>
 *
 * <p>The verifier and the throttle under test are the real implementations; only the two persistence
 * ports are in memory. The subject is the combination — attempt counted, block enforced, success
 * cleared — which is precisely what was missing.</p>
 */
class SecondFactorThrottleSpec extends Specification {

  static final String USER_ID = "u-1"
  static final String OTHER_USER_ID = "u-2"
  static final String SEED = Totp.newSeed()
  static final int MAX_ATTEMPTS = 5
  static final Duration BLOCK = Duration.ofMinutes(15)
  static final String WRONG_CODE = "000000"

  FakeFactors factors = new FakeFactors()
  InMemoryCounter counter = new InMemoryCounter()
  AuthAttemptThrottle throttle
  SecondFactorVerificationService verificationService

  Instant now = Instant.now()

  def setup() {
    factors.save(SecondFactor.enrolTotp(USER_ID, protectedSecret(), "phone", now))
    // Only the secret protector is a stand-in: the seed is supplied as plaintext here, and its
    // encryption is covered by its own spec. Verification and throttling are the real code.
    throttle = new AuthAttemptThrottle(counter,
        10, BLOCK, BLOCK,
        5, BLOCK, BLOCK,
        MAX_ATTEMPTS, BLOCK, BLOCK)
    // The service owns the throttle now, so the page cannot forget to consult it — the defect.
    verificationService = new SecondFactorVerificationService(factors, new PlaintextProtector(),
        throttle)
  }

  // ---------------------------------------------------------------- the reported defect

  def "a burst of wrong codes stops being accepted once the attempt limit is reached"() {
    given:
    def attempts = []
    20.times { attempts << submit(USER_ID, WRONG_CODE, now) }

    expect: "exactly the configured number reached the verifier"
    attempts.count { it.throttled() } == 20 - MAX_ATTEMPTS

    and: "the rest were refused as throttle refusals, before any comparison"
    attempts.drop(MAX_ATTEMPTS)
        .every { it.outcome() == SecondFactorVerificationService.Outcome.THROTTLED }

    and: "no wrong code was ever accepted"
    attempts.every { !it.verified() }
  }

  def "the block holds across the next TOTP step, not just the next click"() {
    given:
    (MAX_ATTEMPTS + 1).times { submit(USER_ID, WRONG_CODE, now) }

    expect: "thirty seconds later — a fresh step — the account is still throttled"
    submit(USER_ID, WRONG_CODE, now.plusSeconds(30)).throttled()

    and: "and one minute before the block expires"
    submit(USER_ID, WRONG_CODE, now.plus(BLOCK.minusSeconds(60))).throttled()
  }

  def "the reported symptom: two minutes of clicking at the challenge stops"() {
    given: "twenty submissions stand in for two minutes of clicking"
    def outcomes = (1..20).collect { submit(USER_ID, WRONG_CODE, now.plusSeconds(it * 6L)) }

    expect: "the later ones are refused, so the guesses are bounded by the policy, not by stamina"
    outcomes.count { it.outcome() == SecondFactorVerificationService.Outcome.THROTTLED } > 0
    outcomes.count { !it.verified() } == 20
  }

  def "the throttle lifts once the block duration has passed"() {
    given:
    (MAX_ATTEMPTS + 1).times { submit(USER_ID, WRONG_CODE, now) }

    when:
    def later = submit(USER_ID, WRONG_CODE, now.plus(BLOCK).plusSeconds(1))

    then: "the legitimate user is not locked out permanently"
    !later.throttled()

    and: "the submission was actually verified against the factor again"
    !later.verified()
  }

  // ---------------------------------------------------------------- scoping

  def "one account's failures do not throttle another account"() {
    given: "one account exhausts its allowance"
    (MAX_ATTEMPTS + 1).times { submit(USER_ID, WRONG_CODE, now) }
    factors.save(SecondFactor.enrolTotp(OTHER_USER_ID, protectedSecret(), "phone", now))

    when: "a different account submits a code"
    def other = submit(OTHER_USER_ID, WRONG_CODE, now)

    then: "the bucket is per account, so a victim cannot be silenced by attacking someone else"
    !other.throttled()
  }

  def "second-factor attempts are counted separately from password attempts"() {
    given: "the password bucket for the same identifier is exhausted"
    (MAX_ATTEMPTS + 1).times { throttle.recordAttempt(AuthAttemptThrottle.Scope.LOGIN, USER_ID, now) }

    expect: "second-factor submissions have their own budget"
    !submit(USER_ID, WRONG_CODE, now).throttled()
  }

  def "a different wrong guess is refused exactly the same"() {
    given: "an attacker varies the code in case the refusal is per-code"
    (MAX_ATTEMPTS + 1).times { submit(USER_ID, WRONG_CODE, now) }

    expect:
    submit(USER_ID, "111111", now.plusSeconds(1)).throttled()
  }

  // ---------------------------------------------------------------- the successful case

  def "a valid code is accepted just under the limit"() {
    given: "the user mistyped one short of the limit"
    (MAX_ATTEMPTS - 1).times { submit(USER_ID, WRONG_CODE, now) }

    expect: "a real user is never refused a correct code because of their own typos"
    submit(USER_ID, Totp.code(SEED, now), now).verified()
  }

  def "a successful verification clears the account's failure count"() {
    given:
    (MAX_ATTEMPTS - 1).times { submit(USER_ID, WRONG_CODE, now) }
    assert submit(USER_ID, Totp.code(SEED, now), now).verified()

    when: "the user then mistypes again, in the same window"
    def nextTypos = (1..MAX_ATTEMPTS).collect { submit(USER_ID, WRONG_CODE, now) }

    then: "the allowance started over, so none of these was throttled"
    nextTypos.every { !it.throttled() }
  }

  def "a throttled account is refused even when it finally presents the correct code"() {
    given: "the allowance is spent"
    (MAX_ATTEMPTS + 1).times { submit(USER_ID, WRONG_CODE, now) }

    expect: "the throttle is checked first — otherwise a blocked attacker only needs patience"
    submit(USER_ID, Totp.code(SEED, now), now).throttled()
  }

  // ---------------------------------------------------------------- helpers

  /**
   * What the challenge page must do: consult the throttle, verify, then record or clear.
   *
   * <p>{@code throttled} and {@code verified} are distinct on purpose. A wrong code is refused by
   * the verifier and counted; a throttled submission never reaches the verifier at all. Collapsing
   * the two is how a spec ends up asserting that a rate limit works when only rejection does.</p>
   */
  /**
   * The page's whole job now: hand the code to the service and read the outcome.
   *
   * <p>Deliberately does <em>not</em> consult the throttle itself. The defect was that the page was
   * the only place that could have, and did not — so the control has to live underneath it, where a
   * future view cannot forget it.</p>
   */
  private Outcome submit(String userId, String code, Instant at) {
    def result = verificationService.verify(userId, code, at)
    return new Outcome(result.outcome() == SecondFactorVerificationService.Outcome.THROTTLED,
        result.verified(), result.outcome())
  }

  record Outcome(boolean throttled, boolean verified,
                 SecondFactorVerificationService.Outcome outcome) {
  }

  static ProtectedSecret protectedSecret() {
    byte[] padded = new byte[60]
    return ProtectedSecret.ofBase64(Base64.encoder.encodeToString(padded))
  }

  /** Reveals the spec's single seed unchanged: there is nothing to decrypt in this fixture. */
  static class PlaintextProtector implements SecondFactorSecretProtector {
    @Override
    String protect(char[] plaintext) {
      return new String(plaintext)
    }

    @Override
    char[] reveal(String protectedSecret) {
      return SEED.toCharArray()
    }
  }

  /**
   * The counter port in memory, modelled on the SQL adapter's documented behaviour.
   *
   * <p>The adapter rolls the window over when the row's own window end has passed, and the upsert
   * clears {@code blockedUntil} in the same {@code CASE} — so the <em>window</em>, not a sweeper job
   * and not a separate block-duration check, is what ends a block. A simpler fake (keep the row
   * forever) would make "the throttle lifts" fail while the production code is correct, which is the
   * most expensive kind of test bug.</p>
   *
   * <p>Modelled faithfully, including the detail that a row is reset <em>while blocked</em> once its
   * window has elapsed: the real statement does not short-circuit on {@code blockedUntil}.</p>
   */
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

  static class FakeFactors implements SecondFactorRepository {
    final List<SecondFactor> stored = []

    @Override
    SecondFactor save(SecondFactor factor) {
      stored << factor
      return factor
    }

    @Override
    Optional<SecondFactor> findById(long id) {
      Optional.empty()
    }

    @Override
    List<SecondFactor> findAllByUserId(String userId) {
      stored.findAll { it.userId() == userId }
    }

    @Override
    List<SecondFactor> findActiveByUserId(String userId) {
      stored.findAll { it.userId() == userId && it.isActive() }
    }

    @Override
    boolean hasActiveFactor(String userId) {
      !findActiveByUserId(userId).isEmpty()
    }
  }
}
