package life.qbic.identity.application.verification;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import life.qbic.identity.domain.model.verification.TokenPurpose;
import life.qbic.identity.domain.repository.VerificationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Invalidates outstanding password-reset links after a completed password change.</b>
 *
 * <p>Implements the second half of {@code AUTH-R-05} / story #1674: a user who requests two resets
 * and uses the second must not leave the first valid, or an attacker holding the older link can
 * still take the account over.</p>
 *
 * <h3>Why this is a bean and not a lambda in the wiring</h3>
 * <p>{@code IdentityService} is a {@code final} class constructed by an {@code @Bean} method, so it
 * cannot be proxied and its own methods cannot carry a transaction. The invalidation is a JPQL bulk
 * {@code UPDATE} on a container-managed {@code EntityManager}, which throws
 * {@link jakarta.persistence.TransactionRequiredException} when no transaction is active. Wiring the
 * repository call as a lambda therefore produced a real production failure:</p>
 *
 * <pre>
 * HTTP 500 · No active transaction for update or delete query
 *   at JpaVerificationTokenRepository.supersedeAllFor
 *   at AppConfig.lambda$userRegistrationService$0
 *   at IdentityService.newUserPassword
 * </pre>
 *
 * <p>It failed <em>after</em> the new password had been committed, so the visitor saw an error page
 * for a change that had actually succeeded, and the outstanding links stayed valid. The
 * transaction boundary belongs to a bean Spring can proxy, which is this class.</p>
 *
 * <h3>Transaction scope</h3>
 * <p>The invalidation commits separately from the password update, because the latter goes through
 * {@code UserRepository} and has already committed by the time this runs. A crash in between leaves
 * the new password in place and the old links live until they expire — the pre-change behaviour, not
 * a new weakness. Making both one unit of work would require a transaction span around
 * {@code IdentityService}, which the {@code final} class prevents.</p>
 *
 * @since 1.20.2
 */
@Service
public class PasswordResetTokenInvalidator {

  private final VerificationTokenRepository verificationTokenRepository;

  public PasswordResetTokenInvalidator(VerificationTokenRepository verificationTokenRepository) {
    this.verificationTokenRepository = requireNonNull(verificationTokenRepository,
        "verificationTokenRepository must not be null");
  }

  /**
   * Supersedes every still-usable password-reset token of the account.
   *
   * <p>Must run inside a transaction; the annotation on this method is what provides it when the
   * method is called through the Spring proxy, which is how production reaches it.</p>
   *
   * @param userId the account whose outstanding reset links are to be invalidated
   * @return the number of tokens superseded
   */
  @Transactional
  public int invalidateOutstanding(String userId) {
    requireNonNull(userId, "userId must not be null");
    return verificationTokenRepository.supersedeAllFor(userId, TokenPurpose.PASSWORD_RESET, null,
        Instant.now());
  }
}
