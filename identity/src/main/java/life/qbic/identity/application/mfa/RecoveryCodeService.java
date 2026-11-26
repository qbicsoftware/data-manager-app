package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.List;
import java.time.Duration;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.identity.domain.model.mfa.RecoveryCode;
import life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder;
import life.qbic.identity.domain.model.mfa.RecoveryCodes;
import life.qbic.identity.domain.repository.RecoveryCodeRepository;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Recovery codes: generate once, redeem once, never reconstructable (AUTH-R-03)</b>
 *
 * <p>This is the path out of the most common lockout — a lost or replaced phone — without a support
 * ticket, and without a permanent exemption anywhere in the login path (ADR 0011 B1).</p>
 *
 * <p><b>Regeneration retires the previous set in the same transaction.</b> Generating a new set and
 * invalidating the old one in two units of work would leave a window where both sets work, or a crash
 * where neither does; the repository operations are therefore called inside one
 * {@code @Transactional} boundary.</p>
 *
 * <p><b>Redemption never says why it failed.</b> Unknown code, already used, retired set: all report
 * the same "not accepted" (AUTH-NFR-03), because the alternatives distinguish "this code existed" from
 * "this code did not", which is exactly what an attacker wants to learn.</p>
 *
 * @since 1.0.0
 */
@Service
public class RecoveryCodeService {

  private static final Logger log = logger(RecoveryCodeService.class);

  private final RecoveryCodeRepository recoveryCodeRepository;
  private final RecoveryCodeEncoder recoveryCodeEncoder;
  private final AuthAttemptThrottle throttle;

  @Autowired
  public RecoveryCodeService(RecoveryCodeRepository recoveryCodeRepository,
      RecoveryCodeEncoder recoveryCodeEncoder, AuthAttemptThrottle throttle) {
    this.recoveryCodeRepository = requireNonNull(recoveryCodeRepository,
        "recoveryCodeRepository must not be null");
    this.recoveryCodeEncoder = requireNonNull(recoveryCodeEncoder,
        "recoveryCodeEncoder must not be null");
    this.throttle = requireNonNull(throttle, "throttle must not be null");
  }

  /**
   * Generates a fresh set and retires every unused code of the previous one.
   *
   * @param userId the account
   * @param now    the generation instant
   * @return the plaintext codes, to be shown exactly once
   */
  @Transactional
  public RecoveryCodeSet generate(String userId, Instant now) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(now, "now must not be null");

    List<String> rawCodes = RecoveryCodes.generate();
    int retired = recoveryCodeRepository.invalidateAllFor(userId, now);
    recoveryCodeRepository.saveAll(rawCodes.stream()
        .map(code -> RecoveryCode.issue(userId, recoveryCodeEncoder.hash(code), now))
        .toList());

    // Counts only: the codes themselves must never reach a log appender.
    log.info("Generated %d recovery codes for user %s (%d previous codes retired)"
        .formatted(rawCodes.size(), userId, retired));
    return new RecoveryCodeSet(userId, rawCodes, now);
  }

  /**
   * Redeems a presented code.
   *
   * <p>The account is not an input: the code is looked up by its hash, which is what makes this usable
   * by someone who cannot log in. The lookup-and-consume is atomic in the repository, so two
   * simultaneous submissions of the same code cannot both succeed.</p>
   *
   * @param presentedCode the code the user typed, in any of the accepted shapes
   * @param now           the instant of redemption
   * @return {@code true} if the code was unused and unretired; it has then been spent
   */
  @Transactional
  public boolean redeem(String presentedCode, Instant now) {
    requireNonNull(presentedCode, "presentedCode must not be null");
    requireNonNull(now, "now must not be null");
    String hash;
    try {
      hash = recoveryCodeEncoder.hash(presentedCode);
    } catch (IllegalArgumentException e) {
      // Wrong length or a character outside the alphabet: a failed attempt, not an error to surface.
      return false;
    }
    boolean redeemed = recoveryCodeRepository.consumeIfUsable(hash, now).isPresent();
    if (redeemed) {
      // No user id here on purpose: the code is the only identifier available, and logging which
      // account it belonged to would require resolving it just to write a log line.
      log.info("A recovery code was redeemed");
    } else {
      log.warn("A recovery code was refused (unknown, already used, or retired)");
    }
    return redeemed;
  }

  /**
   * Redeems a code presented at the login challenge, where the account is already known.
   *
   * <p>Same uniform refusal as {@link #redeem}, plus one rule that only exists on this path: the code
   * must belong to the account that is mid-login. A code from another account must neither
   * authenticate nor be spent — burning a stranger's code would be a denial of service against
   * someone who may still need it.</p>
   *
   * @param userId        the account whose password was already verified
   * @param presentedCode the code the user typed
   * @param now           the instant of redemption
   * @return {@code true} if the code belonged to that account, was unused, and has now been spent
   */
  @Transactional
  public Redemption redeemFor(String userId, String presentedCode, Instant now) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(presentedCode, "presentedCode must not be null");
    requireNonNull(now, "now must not be null");

    // Same budget as the authenticator code, deliberately. A recovery code is twelve characters from
    // a 32-symbol alphabet (~2^60) and stays valid until redeemed, so it is the *more* attractive
    // secret to guess at, and the first version of the MFA-68 fix left exactly this path unguarded.
    // One bucket, not two: separate buckets would let an attacker spend five of each per window
    // against the same login (MFA-68 follow-up).
    if (throttle.record(AuthAttemptThrottle.Scope.SECOND_FACTOR, userId, now)
        == AuthAttemptThrottle.Decision.THROTTLED) {
      log.warn("Recovery-code redemption refused for user %s: attempt limit reached"
          .formatted(userId));
      return new Redemption(Outcome.THROTTLED, throttle.blockDuration(
          AuthAttemptThrottle.Scope.SECOND_FACTOR));
    }

    String hash;
    try {
      hash = recoveryCodeEncoder.hash(presentedCode);
    } catch (IllegalArgumentException e) {
      // Wrong length or a character outside the alphabet: a failed attempt, already counted above.
      return new Redemption(Outcome.REJECTED, null);
    }
    boolean redeemed = recoveryCodeRepository
        .consumeIfUsableForUser(hash, userId, now)
        .isPresent();
    if (redeemed) {
      // The account is in again: its allowance starts over.
      throttle.onSuccess(AuthAttemptThrottle.Scope.SECOND_FACTOR, userId);
      log.info("A recovery code was redeemed at the login challenge for user %s"
          .formatted(userId));
      return new Redemption(Outcome.REDEEMED, null);
    }
    log.warn("A recovery code was refused at the login challenge for user %s".formatted(userId));
    return new Redemption(Outcome.REJECTED, null);
  }

  /**
   * Why a redemption ended as it did.
   *
   * <p>{@link #REDEEMED} and {@link #REJECTED} stay deliberately indistinguishable in the response —
   * unknown, already used, retired and belonging-to-someone-else all report {@code REJECTED}
   * (AUTH-NFR-03). {@link #THROTTLED} is the exception, and it is a deliberate one.</p>
   *
   * <h3>Why the throttle is the one outcome the user is told about</h3>
   * <p>Hiding it protects nothing here. Reaching the limit requires the correct password for this
   * account, so the caller already knows the account exists — the enumeration oracle the uniform
   * response defends against is not in play. Meanwhile the cost of silence is high and falls entirely
   * on the legitimate user: they sit on a challenge page typing codes that can no longer succeed,
   * being told each time that the code was wrong. That is the difference between a rate limit and a
   * dead end.</p>
   *
   * <p>The rule that a throttled response must be indistinguishable (AUTH-NFR-03, throttle Rule 2)
   * still holds where it was written for: the password login and the resend flows, which are reachable
   * anonymously. This path is not.</p>
   *
   * @param outcome    what happened
   * @param retryAfter how long the block lasts, when {@code outcome == THROTTLED}; else {@code null}
   */
  public record Redemption(Outcome outcome, Duration retryAfter) {

    public Redemption {
      requireNonNull(outcome, "outcome must not be null");
    }

    public boolean redeemed() {
      return outcome == Outcome.REDEEMED;
    }

    public boolean throttled() {
      return outcome == Outcome.THROTTLED;
    }
  }

  /** The three answers a redemption can give. */
  public enum Outcome {
    /** The code belonged to the account, was unused, and has now been spent. */
    REDEEMED,
    /** Refused, for any of the reasons the response must not distinguish (AUTH-NFR-03). */
    REJECTED,
    /** The account has used up its attempts in the current window. */
    THROTTLED
  }

  /**
   * @param userId the account
   * @return how many codes are still usable, for the "N codes left" hint (M3-3)
   */
  public long remaining(String userId) {
    requireNonNull(userId, "userId must not be null");
    return recoveryCodeRepository.countUsableByUserId(userId);
  }
}
