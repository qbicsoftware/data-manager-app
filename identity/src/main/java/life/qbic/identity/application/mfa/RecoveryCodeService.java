package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.List;
import life.qbic.identity.domain.model.mfa.RecoveryCode;
import life.qbic.identity.domain.model.mfa.RecoveryCodeEncoder;
import life.qbic.identity.domain.model.mfa.RecoveryCodes;
import life.qbic.identity.domain.repository.RecoveryCodeRepository;
import life.qbic.logging.api.Logger;
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

  public RecoveryCodeService(RecoveryCodeRepository recoveryCodeRepository,
      RecoveryCodeEncoder recoveryCodeEncoder) {
    this.recoveryCodeRepository = requireNonNull(recoveryCodeRepository,
        "recoveryCodeRepository must not be null");
    this.recoveryCodeEncoder = requireNonNull(recoveryCodeEncoder,
        "recoveryCodeEncoder must not be null");
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
   * @param userId the account
   * @return how many codes are still usable, for the "N codes left" hint (M3-3)
   */
  public long remaining(String userId) {
    requireNonNull(userId, "userId must not be null");
    return recoveryCodeRepository.countUsableByUserId(userId);
  }
}
