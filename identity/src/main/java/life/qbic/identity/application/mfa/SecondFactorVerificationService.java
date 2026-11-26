package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import life.qbic.identity.domain.model.mfa.SecondFactor;
import life.qbic.identity.domain.model.mfa.SecondFactorType;
import life.qbic.identity.domain.model.mfa.Totp;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.identity.domain.repository.SecondFactorRepository;
import life.qbic.logging.api.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Verifying a presented second factor at login (AUTH-R-01)</b>
 *
 * <p>The only place a code is accepted. It answers one question — "did this account present a valid
 * factor just now" — and records the use so the settings page can show it. It deliberately does not
 * know about sessions, redirects or policies: the login filter decides <em>whether</em> to ask, this
 * decides <em>whether the answer was good</em>.</p>
 *
 * <h3>Fail closed, per attempt</h3>
 * <p>A seed that cannot be decrypted (vault key missing, rotated, or a corrupted row) never counts as
 * "no factor enrolled" and never as success: the attempt is reported
 * {@link Outcome#UNAVAILABLE} and the caller must deny. That is AUTH-NFR-02 — a factor whose backing
 * store is broken must not become a bypass.</p>
 *
 * <p>Several factors are tried in turn, newest first, so a user with a phone and a tablet is not
 * locked out by their older device. A code is accepted against at most one factor, and the use is
 * recorded on that one.</p>
 *
 * <h3>Throttled per account (MFA-68)</h3>
 * <p>A six-digit code is one guess in a million per thirty-second step. That is only a control while
 * the attacker gets <em>one</em> guess per step: without a rate limit, a million scripted submissions
 * turn the second factor into a formality. The QA run found exactly that — two minutes of clicking at
 * the challenge page, nothing stopping it (MFA-68).</p>
 *
 * <p>The attempt is therefore recorded before the code is looked at, and cleared when a code is
 * accepted, so a user who mistypes and then succeeds starts from zero. The budget is keyed on the
 * account id: the password step already established which account is mid-login, and one account's
 * failures must not spend another's allowance.</p>
 *
 * <p><b>The refusal is deliberately indistinguishable from a wrong code</b> (AUTH-NFR-03). The caller
 * renders its single "not accepted" sentence for both. An attacker holding the password is the exact
 * audience a distinct "too many attempts" would help.</p>
 *
 * @since 1.0.0
 */
@Service
public class SecondFactorVerificationService {

  private static final Logger log = logger(SecondFactorVerificationService.class);

  private final SecondFactorRepository secondFactorRepository;
  private final SecondFactorSecretProtector secretProtector;
  private final AuthAttemptThrottle throttle;

  public SecondFactorVerificationService(SecondFactorRepository secondFactorRepository,
      SecondFactorSecretProtector secretProtector,
      AuthAttemptThrottle throttle) {
    this.secondFactorRepository = requireNonNull(secondFactorRepository,
        "secondFactorRepository must not be null");
    this.secretProtector = requireNonNull(secretProtector, "secretProtector must not be null");
    this.throttle = requireNonNull(throttle, "throttle must not be null");
  }

  /**
   * Verifies a presented code against the account's active factors.
   *
   * @param userId the account whose password was just verified
   * @param code   the code the user typed
   * @param now    the instant of verification
   * @return the outcome; only {@link Outcome#VERIFIED} may establish a session
   */
  @Transactional
  public Result verify(String userId, String code, Instant now) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(code, "code must not be null");
    requireNonNull(now, "now must not be null");

    // Recorded first, so the attempt is charged even if verification fails a step later, and so the
    // configured number of attempts is exactly what a visitor gets per window. A success clears the
    // bucket below.
    if (throttle.recordAttempt(AuthAttemptThrottle.Scope.SECOND_FACTOR, userId, now)
        == AuthAttemptThrottle.Decision.THROTTLED) {
      log.warn("Second-factor verification refused for user %s: attempt limit reached"
          .formatted(userId));
      return new Result(Outcome.THROTTLED, null);
    }

    List<SecondFactor> factors = secondFactorRepository.findActiveByUserId(userId);
    if (factors.isEmpty()) {
      // Reachable if a factor was removed in another tab between the password and the code, or if
      // the policy changed mid-flight. It is not an error; it means "this account cannot pass".
      log.warn("Second-factor verification requested for user %s without an active factor"
          .formatted(userId));
      return new Result(Outcome.NO_FACTOR, null);
    }

    boolean decryptionFailed = false;
    for (SecondFactor factor : factors) {
      if (factor.type() != SecondFactorType.TOTP) {
        // WebAuthn assertions are verified by their own path (M4-2); a code cannot satisfy them.
        continue;
      }
      char[] seed;
      try {
        seed = secretProtector.reveal(factor.secret()
            .orElseThrow(() -> new IllegalStateException(
                "a TOTP factor without a secret cannot be verified (factor "
                    + factor.id() + ")"))
            .encoded());
      } catch (SecondFactorSecretUnavailableException | IllegalStateException e) {
        log.error("Second factor %s of user %s cannot be decrypted; treating the attempt as failed"
            .formatted(factor.id(), userId), e);
        decryptionFailed = true;
        continue;
      }
      try {
        if (Totp.verify(new String(seed), code, now)) {
          factor.markUsed(now);
          secondFactorRepository.save(factor);
          // The account is in again: its allowance starts over, so a later typo is not still the
          // sixth attempt of a window that began before this login.
          throttle.onSuccess(AuthAttemptThrottle.Scope.SECOND_FACTOR, userId);
          log.info("Second factor %s of user %s verified".formatted(factor.id(), userId));
          return new Result(Outcome.VERIFIED, factor.id());
        }
      } finally {
        Arrays.fill(seed, '\0');
      }
    }

    // No code in the message, and no indication of which factor (if any) was close.
    log.warn("Second factor verification failed for user %s".formatted(userId));
    return new Result(decryptionFailed ? Outcome.UNAVAILABLE : Outcome.REJECTED, null);
  }

  /** Why verification ended as it did. Distinct outcomes for the caller, one message for the user. */
  public enum Outcome {
    /** A factor matched; the account may proceed. */
    VERIFIED,
    /** The code did not match any active factor. */
    REJECTED,
    /** No active factor exists (removed concurrently, or never enrolled). */
    NO_FACTOR,
    /**
     * The account has used up its attempts in the current window (MFA-68).
     *
     * <p>Distinct for the caller's control flow only — {@code verified()} is {@code false} for it, so
     * every existing caller already denies. It exists so the reason is visible in a log and a test,
     * not so a view can describe it.</p>
     */
    THROTTLED,
    /** At least one factor could not be decrypted; fail closed (AUTH-NFR-02). */
    UNAVAILABLE
  }

  /**
   * @param outcome  why verification ended as it did
   * @param factorId the factor that matched, when {@code outcome == VERIFIED}
   */
  public record Result(Outcome outcome, Long factorId) {

    public Result {
      requireNonNull(outcome, "outcome must not be null");
    }

    public boolean verified() {
      return outcome == Outcome.VERIFIED;
    }
  }
}
