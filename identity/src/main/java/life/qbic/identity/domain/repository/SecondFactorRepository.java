package life.qbic.identity.domain.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.SecondFactor;

/**
 * <b>Second factor repository</b>
 *
 * <p>Persistence port for {@link SecondFactor}. Implemented in {@code identity-infrastructure}.</p>
 *
 * <p>The read methods are split by intent rather than by convenience, because the two callers need
 * different things and mixing them is how a disabled factor ends up being counted as enrolment:</p>
 * <ul>
 *   <li>the login path asks {@link #hasActiveFactor} — disabled factors must not count, and the
 *       answer has to be cheap;</li>
 *   <li>the settings surface asks {@link #findAllByUserId} — a user must be able to see the factor
 *       they removed, otherwise "I removed it and it still shows up" and "I removed it and it is
 *       gone" look identical.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface SecondFactorRepository {

  /**
   * @return the persisted, managed instance. Callers must use the returned value rather than the
   *     argument: {@code merge} leaves the argument detached, so mutating it afterwards would not be
   *     flushed.
   */
  SecondFactor save(SecondFactor factor);

  Optional<SecondFactor> findById(long id);

  /**
   * @param userId the account
   * @return every factor of the account, newest first, <b>including disabled ones</b>
   */
  List<SecondFactor> findAllByUserId(String userId);

  /**
   * @param userId the account
   * @return the factors that may be presented, newest first
   */
  List<SecondFactor> findActiveByUserId(String userId);

  /**
   * The question the login path asks (AUTH-R-02: enrolment means an <em>active</em> factor).
   *
   * @param userId the account
   * @return {@code true} if the account has at least one factor that is not disabled
   */
  boolean hasActiveFactor(String userId);
}
