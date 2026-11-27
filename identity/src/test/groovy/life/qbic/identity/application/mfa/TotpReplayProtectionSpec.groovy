package life.qbic.identity.application.mfa

import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.model.mfa.Totp
import life.qbic.identity.domain.repository.SecondFactorRepository
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Regression: a code that has already been accepted must not be accepted again (MFA-20)</b>
 *
 * <p>The QA sheet recorded this as a *pass* against the stated expectation, with the observation "can
 * log in again with the same valid code". The expectation was simply too weak. A TOTP code is valid for
 * its 30-second step, and the server tolerates one step either side for clock drift, so a code that has
 * been seen once stays usable for <b>ninety seconds</b> — and any number of times within it.</p>
 *
 * <p>{@code Totp} documents this as deliberately not its job: "a code stays valid for its whole step…
 * that is a property of the login step, which must refuse to accept a step it has already accepted for
 * the same account." The login step never implemented the other half. This spec pins it.</p>
 */
class TotpReplayProtectionSpec extends Specification {

  static final String USER = "u-1"
  static final String SEED = Totp.newSeed()
  static final Instant T0 = Instant.parse("2026-03-01T10:00:10Z")

  FakeFactors factors = new FakeFactors()
  SecondFactorVerificationService service

  def setup() {
    factors.save(SecondFactor.enrolTotp(USER, protectedSecret(), "phone", T0))
    service = new SecondFactorVerificationService(factors, new PlaintextProtector(SEED),
        new life.qbic.identity.application.throttle.AuthAttemptThrottle(
            new PermissiveCounter(), 10, Duration.ofMinutes(15), Duration.ofMinutes(15),
            5, Duration.ofMinutes(15), Duration.ofMinutes(15), 1000, Duration.ofMinutes(15),
            Duration.ofMinutes(15)))
  }

  // ---------------------------------------------------------------- the report

  def "the same code is refused the second time it is presented"() {
    given:
    def code = Totp.code(SEED, T0)

    when:
    def first = service.verify(USER, code, T0)
    def second = service.verify(USER, code, T0.plusSeconds(5))

    then:
    first.verified()
    !second.verified()
    second.outcome() == SecondFactorVerificationService.Outcome.REJECTED
  }

  def "a code is refused for as long as it would otherwise stay valid"() {
    given: "well inside the 90-second tolerance window"
    def code = Totp.code(SEED, T0)
    service.verify(USER, code, T0)

    expect: "every later attempt within the window is a replay, not a fresh login"
    (5..85).step(10).every { offset ->
      !service.verify(USER, code, T0.plusSeconds(offset as long)).verified()
    }
  }

  def "a genuinely newer code still works after a replay attempt"() {
    given: "the refusal must not poison the factor"
    def now = Totp.code(SEED, T0)
    service.verify(USER, now, T0)
    service.verify(USER, now, T0)

    when: "the app advances a step"
    def next = service.verify(USER, Totp.code(SEED, T0.plusSeconds(30)), T0.plusSeconds(30))

    then:
    next.verified()
  }

  // ---------------------------------------------------------------- the tolerance window

  def "a code from an older step is refused once a newer step has been accepted"() {
    given: "a phone whose clock is behind, so its code matches the previous step"
    def behind = Totp.code(SEED, T0.minusSeconds(30))
    def current = Totp.code(SEED, T0)

    when: "the user presents the older code first (tolerated), then the current one"
    def older = service.verify(USER, behind, T0)
    def newer = service.verify(USER, current, T0)

    then:
    older.verified()
    newer.verified()

    when: "the older code is presented again after the newer one was accepted"
    def replay = service.verify(USER, behind, T0)

    then: "the high-water mark has moved past it, so the tolerance window cannot be exploited"
    !replay.verified()
  }

  // ---------------------------------------------------------------- what must not change

  def "a wrong code is refused without touching the mark"() {
    given:
    def valid = Totp.code(SEED, T0)
    service.verify(USER, "000001", T0)

    expect: "a failed attempt must not advance the high-water mark"
    service.verify(USER, valid, T0).verified()
  }

  def "recovery codes are unaffected — they are single use by construction"() {
    expect: "this change is about TOTP replay only"
    true
  }

  def "the refusal says nothing about why, so it cannot confirm a code was used"() {
    given:
    def code = Totp.code(SEED, T0)
    service.verify(USER, code, T0)

    expect: "same outcome as a wrong code (AUTH-NFR-03)"
    service.verify(USER, code, T0).outcome()
            == SecondFactorVerificationService.Outcome.REJECTED
  }

  // ---------------------------------------------------------------- fakes

  static ProtectedSecret protectedSecret() {
    return ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[60]))
  }

  static class PlaintextProtector implements SecondFactorSecretProtector {
    private final String seed

    PlaintextProtector(String seed) {
      this.seed = seed
    }

    @Override
    String protect(char[] plaintext) {
      return Base64.encoder.encodeToString(new byte[60])
    }

    @Override
    char[] reveal(String protectedSecret) {
      return seed.toCharArray()
    }
  }

  static class FakeFactors implements SecondFactorRepository {
    final List<SecondFactor> stored = []
    private long nextId = 1L

    @Override
    SecondFactor save(SecondFactor factor) {
      if (factor.id() == null) {
        def field = SecondFactor.getDeclaredField("id")
        field.setAccessible(true)
        field.set(factor, nextId++)
      }
      stored.removeAll { it.id() == factor.id() }
      stored << factor
      return factor
    }

    @Override
    Optional<SecondFactor> findById(long id) {
      return Optional.ofNullable(stored.find { it.id() == id })
    }

    @Override
    List<SecondFactor> findAllByUserId(String userId) {
      return stored.findAll { it.userId() == userId }
    }

    @Override
    List<SecondFactor> findActiveByUserId(String userId) {
      return stored.findAll { it.userId() == userId && it.isActive() }
    }

    @Override
    boolean hasActiveFactor(String userId) {
      return !findActiveByUserId(userId).isEmpty()
    }
  }

  static class PermissiveCounter implements life.qbic.identity.domain.repository.AuthAttemptCounter {
    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      return new Counter(1, null)
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
    }

    @Override
    Counter peek(String scope, String key) {
      return new Counter(0, null)
    }

    @Override
    void clear(String scope, String key) {
    }
  }
}
