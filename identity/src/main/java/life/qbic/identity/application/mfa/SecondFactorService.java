package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.SecondFactor;
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

  public SecondFactorService(SecondFactorRepository secondFactorRepository) {
    this.secondFactorRepository = requireNonNull(secondFactorRepository,
        "secondFactorRepository must not be null");
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
   * Removes one of the account's own factors.
   *
   * <p>A factor that belongs to another account is reported exactly like a factor that does not
   * exist: {@code false}, with no exception and no different timing. The alternative would let a user
   * probe factor ids for existence across accounts.</p>
   *
   * @param userId   the account making the request
   * @param factorId the factor to remove
   * @param now      the removal instant
   * @return {@code true} if a factor of this account was active and has been disabled
   */
  @Transactional
  public boolean disable(String userId, long factorId, Instant now) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(now, "now must not be null");

    Optional<SecondFactor> found = secondFactorRepository.findById(factorId);
    if (found.isEmpty()) {
      return false;
    }
    SecondFactor factor = found.get();
    if (!factor.userId().equals(userId)) {
      log.warn("Refused to disable second factor %d for a user it does not belong to"
          .formatted(factorId));
      return false;
    }
    if (!factor.disable(now)) {
      return false;
    }
    secondFactorRepository.save(factor);
    log.info("Disabled second factor %d of user %s".formatted(factorId, userId));
    return true;
  }
}
