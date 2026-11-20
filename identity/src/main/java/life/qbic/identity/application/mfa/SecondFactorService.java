package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.model.mfa.SecondFactor;
import life.qbic.identity.domain.model.mfa.SecondFactorRemoval;
import life.qbic.identity.domain.model.mfa.SecondFactorRemoval.Outcome;
import life.qbic.identity.domain.repository.RecoveryCodeRepository;
import life.qbic.identity.domain.repository.SecondFactorRepository;
import life.qbic.logging.api.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Managing an account's own second factors (AUTH-R-02)</b>
 *
 * <p>Reads for the settings surface and the one mutation it performs: removing a factor. The account
 * id is always a parameter and always checked against the factor's owner — a factor id from the UI is
 * user input, and "the id exists" must never be enough to act on it.</p>
 *
 * <p><b>Removing the last factor is not an ordinary removal.</b> The decision belongs to
 * {@link SecondFactorRemoval}: under {@code REQUIRED} it is refused, and under {@code OPTIONAL} it is
 * the explicit "turn two-factor authentication off" action, which also retires every recovery code —
 * codes are only meaningful while a factor exists, and leaving live codes behind an account with no
 * factor would be a second, invisible way back into an account that has decided not to have a second
 * step. Both outcomes are visible to the caller instead of being inferred from a bare boolean.</p>
 *
 * <p>Enrolment lives in {@link TotpEnrolmentService}, and verification belongs to the login step
 * (M4): this service deliberately has no "mark as used" method, so a caller cannot record a successful
 * factor use without having verified a code.</p>
 *
 * @since 1.0.0
 */
@Service
public class SecondFactorService {

  private static final Logger log = logger(SecondFactorService.class);

  private final SecondFactorRepository secondFactorRepository;
  private final RecoveryCodeRepository recoveryCodeRepository;
  private final MfaPolicy policy;

  public SecondFactorService(SecondFactorRepository secondFactorRepository,
      RecoveryCodeRepository recoveryCodeRepository, MfaPolicy policy) {
    this.secondFactorRepository = requireNonNull(secondFactorRepository,
        "secondFactorRepository must not be null");
    this.recoveryCodeRepository = requireNonNull(recoveryCodeRepository,
        "recoveryCodeRepository must not be null");
    this.policy = requireNonNull(policy, "policy must not be null");
  }

  /**
   * @param userId the account
   * @return the factors that may be presented, newest first — disabled ones are not listed here
   */
  public List<SecondFactor> activeFactors(String userId) {
    requireNonNull(userId, "userId must not be null");
    return secondFactorRepository.findActiveByUserId(userId);
  }

  /**
   * @param userId the account
   * @return the posture the running application was started with, for the settings surface
   */
  public MfaPolicyMode policyMode() {
    return policy.mode();
  }

  /**
   * How removing one factor would be treated, without removing it.
   *
   * <p>The settings page asks this to disable the button and explain why, instead of letting a user
   * click into a refusal. It uses the same decision the removal itself uses, so the two cannot
   * disagree.</p>
   *
   * @param userId   the account
   * @param factorId the factor the user is looking at
   * @return what removing it would do
   */
  public Outcome removalOutcome(String userId, long factorId) {
    requireNonNull(userId, "userId must not be null");
    return SecondFactorRemoval.decide(policy.mode(), ownsFactor(userId, factorId),
        activeFactors(userId).size());
  }

  /**
   * Removes one of the account's own factors, applying the last-factor rule.
   *
   * <p>When the removal turns two-factor authentication off (last factor under {@code OPTIONAL}), the
   * account's recovery codes are retired in the same transaction. A code that outlives the factor
   * would be a credential for a state the user just left.</p>
   *
   * @param userId   the account making the request
   * @param factorId the factor to remove
   * @param now      the removal instant
   * @return what happened; {@link Outcome#NOT_REMOVABLE} for a factor that does not exist or belongs
   *     to another account, reported identically on purpose
   */
  @Transactional
  public Outcome remove(String userId, long factorId, Instant now) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(now, "now must not be null");

    Optional<SecondFactor> found = secondFactorRepository.findById(factorId);
    boolean owned = found.isPresent() && found.get().userId().equals(userId);
    int activeCount = activeFactors(userId).size();
    Outcome outcome = SecondFactorRemoval.decide(policy.mode(), owned, activeCount);

    if (outcome == Outcome.NOT_REMOVABLE) {
      if (found.isPresent()) {
        // Owned by someone else: worth a warning, worth no detail to the caller.
        log.warn("Refused to remove second factor %d for a user it does not belong to"
            .formatted(factorId));
      }
      return Outcome.NOT_REMOVABLE;
    }
    if (outcome == Outcome.LAST_FACTOR_PROTECTED) {
      log.info("Refused to remove the last second factor %d of user %s under mode %s"
          .formatted(factorId, userId, policy.mode()));
      return Outcome.LAST_FACTOR_PROTECTED;
    }

    SecondFactor factor = found.orElseThrow();
    if (!factor.disable(now)) {
      // Already disabled: nothing changed, and reporting "allowed" would claim a removal that did not
      // happen. The factor is still visible under its owner, so this is not a probe for other accounts.
      return Outcome.NOT_REMOVABLE;
    }
    secondFactorRepository.save(factor);
    log.info("Removed second factor %d of user %s".formatted(factorId, userId));

    if (outcome == Outcome.TURNS_OFF_TWO_FACTOR) {
      int retired = recoveryCodeRepository.invalidateAllFor(userId, now);
      log.info("Two-factor authentication turned off for user %s; %d recovery codes retired"
          .formatted(userId, retired));
    }
    return outcome;
  }

  /**
   * @deprecated use {@link #remove(String, long, Instant)}, which reports what the removal meant and
   *     applies the last-factor rule. Kept only so the call sites can be migrated one at a time.
   */
  @Deprecated(since = "1.0.0")
  @Transactional
  public boolean disable(String userId, long factorId, Instant now) {
    return remove(userId, factorId, now) != Outcome.NOT_REMOVABLE;
  }

  private boolean ownsFactor(String userId, long factorId) {
    return secondFactorRepository.findById(factorId)
        .map(factor -> factor.userId().equals(userId))
        .orElse(false);
  }
}
