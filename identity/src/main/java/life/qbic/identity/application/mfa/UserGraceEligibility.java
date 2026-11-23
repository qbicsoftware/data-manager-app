package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.logging.api.Logger;
import org.springframework.stereotype.Service;

/**
 * <b>Answers {@link GraceEligibility} from the account record and the configured cut-off</b>
 *
 * <p>Two facts, one comparison: when the account was created, and when the posture came into force
 * ({@code qbic.security.mfa.grace-cutoff}). Everything the login path needs to know about who is owed a
 * deadline.</p>
 *
 * <p><b>Every unreadable case answers "eligible".</b> A missing account row, an id that is not a UUID,
 * a failed lookup — none of those is evidence that the account is new, and the only thing a wrong
 * "eligible" can cause is the older, more forgiving path. A wrong "not eligible", by contrast, would
 * send an existing user straight to mandatory enrolment during a partial outage of the account table.
 * The failure is logged at warn because it is a data problem worth seeing.</p>
 *
 * @since 1.0.0
 */
@Service
public class UserGraceEligibility implements GraceEligibility {

  private static final Logger log = logger(UserGraceEligibility.class);

  private final UserRepository userRepository;
  private final MfaPolicy policy;

  public UserGraceEligibility(UserRepository userRepository, MfaPolicy policy) {
    this.userRepository = requireNonNull(userRepository, "userRepository must not be null");
    this.policy = requireNonNull(policy, "policy must not be null");
  }

  @Override
  public boolean isEligible(String userId) {
    requireNonNull(userId, "userId must not be null");
    if (policy.graceCutoff().isEmpty()) {
      // No cut-off configured: nothing has been flipped, so everyone is eligible. Answer without
      // touching the database, which is also the case for every deployment that never sets it.
      return true;
    }
    return policy.graceEligible(registrationDateOf(userId));
  }

  private Instant registrationDateOf(String userId) {
    try {
      return userRepository.findById(UserId.from(userId))
          .map(User::registrationDate)
          .orElse(null);
    } catch (RuntimeException e) {
      log.warn("Could not read the registration date of user %s (%s); treating the account as "
          .formatted(userId, e.getClass().getSimpleName())
          + "eligible for a grace window");
      return null;
    }
  }
}
