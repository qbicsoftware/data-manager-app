package life.qbic.identity.infrastructure.verification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.verification.ConsumeFailure;
import life.qbic.identity.domain.model.verification.TokenPurpose;
import life.qbic.identity.domain.repository.VerificationTokenRepository.ConsumeResult;
import life.qbic.identity.domain.repository.VerificationTokenRepository.Consumed;
import life.qbic.identity.domain.repository.VerificationTokenRepository.Refused;
import life.qbic.identity.domain.model.verification.VerificationToken;
import life.qbic.identity.domain.repository.VerificationTokenRepository;

/**
 * <b>JPA verification token repository</b>
 *
 * <p>Persistence adapter for {@link VerificationToken}.</p>
 *
 * <h3>Why this is written against {@link EntityManager} rather than Spring Data</h3>
 * <p>Two operations here need row locking, which a derived Spring Data query cannot express:</p>
 * <ul>
 *   <li>{@link #findByTokenHashForUpdate} locks the row so two concurrent consumers serialise;</li>
 *   <li>{@link #supersedeAllFor} must not race a concurrent issue for the same (user, purpose).</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class JpaVerificationTokenRepository implements VerificationTokenRepository {

  private final EntityManager entityManager;

  public JpaVerificationTokenRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void save(VerificationToken token) {
    if (token.id() == null) {
      entityManager.persist(token);
    } else {
      entityManager.merge(token);
    }
  }

  @Override
  public Optional<VerificationToken> findByTokenHash(String tokenHash) {
    return entityManager
        .createQuery("SELECT t FROM VerificationToken t WHERE t.tokenHash = :hash",
            VerificationToken.class)
        .setParameter("hash", tokenHash)
        .getResultStream()
        .findFirst();
  }

  /**
   * Consumes a token in one transaction: lock, evaluate, mark used.
   *
   * <p>The lock is only meaningful <em>inside</em> the transaction that performs the update, which is
   * why the whole decision lives here rather than in the service. Splitting it across calls was
   * measured to let all 8 of 8 concurrent consumers succeed.</p>
   *
   * <p>Transaction demarcation: the caller supplies the transaction (in production, a Spring
   * {@code @Transactional} boundary). This adapter deliberately does not begin one, so that
   * consumption and the state change it authorises — confirming the address, setting the password —
   * can share a single transaction and fail together.</p>
   */
  @Override
  public ConsumeResult consumeIfUsable(String tokenHash, TokenPurpose purpose, Instant now) {
    Optional<VerificationToken> locked = findByTokenHashForUpdate(tokenHash);
    if (locked.isEmpty()) {
      return new Refused(ConsumeFailure.UNKNOWN);
    }
    VerificationToken token = locked.get();

    // The rules live on the aggregate; this adapter owns only the transaction and the lock.
    Optional<ConsumeFailure> refusal = token.checkConsumable(purpose, now);
    if (refusal.isPresent()) {
      return new Refused(refusal.get());
    }

    token.consume(now);
    entityManager.merge(token);
    entityManager.flush();
    return new Consumed(token);
  }

  /**
   * Same lookup, but takes a pessimistic write lock on the row.
   *
   * <p>This is the mechanism that makes consumption single-use under concurrency. Without it, two
   * consumers can both read {@code consumedAt = NULL} and both proceed; the second {@code merge}
   * then overwrites the first, and the token is silently usable twice. The unique index on
   * {@code tokenHash} does <em>not</em> help — it guards inserts, not updates.</p>
   */
  public Optional<VerificationToken> findByTokenHashForUpdate(String tokenHash) {
    return entityManager
        .createQuery("SELECT t FROM VerificationToken t WHERE t.tokenHash = :hash",
            VerificationToken.class)
        .setParameter("hash", tokenHash)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .getResultStream()
        .findFirst();
  }

  @Override
  public List<VerificationToken> findAllByUserIdAndPurpose(String userId, TokenPurpose purpose) {
    return entityManager
        .createQuery("""
            SELECT t FROM VerificationToken t
             WHERE t.userId = :userId AND t.purpose = :purpose
             ORDER BY t.createdAt DESC
            """, VerificationToken.class)
        .setParameter("userId", userId)
        .setParameter("purpose", purpose)
        .getResultList();
  }

  @Override
  public int supersedeAllFor(String userId, TokenPurpose purpose, String keepTokenHash,
      Instant now) {
    return entityManager
        .createQuery("""
            UPDATE VerificationToken t
               SET t.supersededAt = :now
             WHERE t.userId = :userId
               AND t.purpose = :purpose
               AND t.supersededAt IS NULL
               AND t.consumedAt IS NULL
               AND t.tokenHash <> :keep
            """)
        .setParameter("now", now)
        .setParameter("userId", userId)
        .setParameter("purpose", purpose)
        .setParameter("keep", keepTokenHash == null ? "" : keepTokenHash)
        .executeUpdate();
  }

  @Override
  public int deleteExpiredOrConsumedBefore(Instant cutoff) {
    return entityManager
        .createQuery("""
            DELETE FROM VerificationToken t
             WHERE t.expiresAt < :cutoff
               AND (t.consumedAt IS NOT NULL OR t.supersededAt IS NOT NULL)
            """)
        .setParameter("cutoff", cutoff)
        .executeUpdate();
  }
}
