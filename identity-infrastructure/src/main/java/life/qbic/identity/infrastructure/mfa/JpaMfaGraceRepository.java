package life.qbic.identity.infrastructure.mfa;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.MfaGrace;
import life.qbic.identity.domain.repository.MfaGraceRepository;
import org.springframework.stereotype.Repository;

/**
 * <b>JPA grace window repository</b>
 *
 * <p>Persistence adapter for {@link MfaGrace}.</p>
 *
 * <p>{@link #startIfAbsent} is a single insert-if-absent statement, not a read followed by a write.
 * The read-then-write version has two failures, and both were the reason the throttling table in
 * {@code auth-attempt-throttling.sql} was written as an upsert:</p>
 * <ul>
 *   <li>two logins of one account can both see "no window" and both insert — one of them fails on the
 *       primary key, turning a normal login into an error;</li>
 *   <li>if it is instead written as "delete then insert", any user can renew their own deadline by
 *       logging in again, and the single deadline the whole enforcement depends on becomes
 *       indefinitely extendable.</li>
 * </ul>
 *
 * <p>{@code ON DUPLICATE KEY UPDATE} is MariaDB/MySQL syntax. H2 accepts it only in
 * {@code MODE=MySQL}, so the persistence spec runs H2 in that mode — otherwise the spec would
 * exercise a statement production never executes.</p>
 *
 * @since 1.0.0
 */
@Repository
public class JpaMfaGraceRepository implements MfaGraceRepository {

  private final EntityManager entityManager;

  public JpaMfaGraceRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Optional<MfaGrace> findByUserId(String userId) {
    return Optional.ofNullable(entityManager.find(MfaGrace.class, userId));
  }

  @Override
  public MfaGrace startIfAbsent(MfaGrace candidate) {
    entityManager.createNativeQuery("""
            INSERT INTO user_mfa_grace (userId, startedAt, graceUntil)
            VALUES (:userId, :startedAt, :graceUntil)
            ON DUPLICATE KEY UPDATE userId = userId
            """)
        .setParameter("userId", candidate.userId())
        .setParameter("startedAt", candidate.startedAt())
        .setParameter("graceUntil", candidate.graceUntil())
        .executeUpdate();

    // Read back instead of returning the candidate: when the row already existed this call changed
    // nothing, and the stored deadline — not the one this caller computed — is the truth.
    //
    // A JPQL query, not find(): if findByUserId was called earlier in this transaction and found
    // nothing, JPA may remember that "absent" result and find() would keep returning empty even
    // though the native insert above just wrote the row.
    List<MfaGrace> stored = entityManager
        .createQuery("SELECT g FROM MfaGrace g WHERE g.userId = :userId", MfaGrace.class)
        .setParameter("userId", candidate.userId())
        .getResultList();
    if (stored.isEmpty()) {
      throw new IllegalStateException(
          "grace window for user " + candidate.userId() + " was not readable after insert");
    }
    return stored.getFirst();
  }
}
