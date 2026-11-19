package life.qbic.identity.infrastructure.mfa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.RecoveryCode;
import life.qbic.identity.domain.repository.RecoveryCodeRepository;
import org.springframework.stereotype.Repository;

/**
 * <b>JPA recovery code repository</b>
 *
 * <p>Persistence adapter for {@link RecoveryCode}.</p>
 *
 * <p>The only non-obvious method is {@link #consumeIfUsable}: it must lock the row inside the
 * consuming transaction, or one code becomes two logins. The usability rule itself stays on the
 * aggregate — this adapter owns the transaction and the lock, and re-implements nothing.</p>
 *
 * @since 1.0.0
 */
@Repository
public class JpaRecoveryCodeRepository implements RecoveryCodeRepository {

  private final EntityManager entityManager;

  public JpaRecoveryCodeRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public List<RecoveryCode> saveAll(List<RecoveryCode> codes) {
    codes.forEach(entityManager::persist);
    return codes;
  }

  @Override
  public List<RecoveryCode> findAllByUserId(String userId) {
    return entityManager
        .createQuery("""
            SELECT c FROM RecoveryCode c
             WHERE c.userId = :userId
             ORDER BY c.createdAt DESC, c.id DESC
            """, RecoveryCode.class)
        .setParameter("userId", userId)
        .getResultList();
  }

  @Override
  public long countUsableByUserId(String userId) {
    return entityManager
        .createQuery("""
            SELECT COUNT(c) FROM RecoveryCode c
             WHERE c.userId = :userId
               AND c.usedAt IS NULL
               AND c.invalidatedAt IS NULL
            """, Long.class)
        .setParameter("userId", userId)
        .getSingleResult();
  }

  @Override
  public int invalidateAllFor(String userId, Instant now) {
    return entityManager
        .createQuery("""
            UPDATE RecoveryCode c
               SET c.invalidatedAt = :now
             WHERE c.userId = :userId
               AND c.usedAt IS NULL
               AND c.invalidatedAt IS NULL
            """)
        .setParameter("now", now)
        .setParameter("userId", userId)
        .executeUpdate();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The lock is taken before the usability rule is evaluated, and the whole thing runs inside the
   * caller's transaction. Splitting it would make the {@code PESSIMISTIC_WRITE} lock released at the
   * end of the read transaction, which is the failure the verification-token adapter measured: every
   * concurrent consumer succeeds.</p>
   */
  @Override
  public Optional<RecoveryCode> consumeIfUsableForUser(String codeHash, String userId,
      Instant now) {
    return consume(codeHash, userId, now);
  }

  @Override
  public Optional<RecoveryCode> consumeIfUsable(String codeHash, Instant now) {
    return consume(codeHash, null, now);
  }

  /**
   * Locks the row by hash, then applies the ownership rule (when a user is given) and the usability
   * rule, then marks it used — all inside the caller's transaction.
   */
  private Optional<RecoveryCode> consume(String codeHash, String userId, Instant now) {
    Optional<RecoveryCode> locked = entityManager
        .createQuery("SELECT c FROM RecoveryCode c WHERE c.codeHash = :hash", RecoveryCode.class)
        .setParameter("hash", codeHash)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .getResultStream()
        .findFirst();
    if (locked.isEmpty() || !locked.get().isUsable()) {
      return Optional.empty();
    }
    RecoveryCode code = locked.get();
    if (userId != null && !code.userId().equals(userId)) {
      // Not this account's code: refuse without spending it (the owner may still need it).
      return Optional.empty();
    }
    code.consume(now);
    // The instance was loaded by THIS EntityManager, so it is managed and the mutation is tracked.
    // merge() would copy it into a second instance and leave us returning the wrong one.
    entityManager.flush();
    return Optional.of(code);
  }
}
