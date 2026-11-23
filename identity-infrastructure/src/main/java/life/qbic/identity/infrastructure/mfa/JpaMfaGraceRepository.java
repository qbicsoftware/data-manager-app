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

  @Override
  public List<MfaGrace> findActiveWithOutstandingReminder(java.time.Instant now) {
    // One query, no per-account work: the job runs on a schedule and must not turn into N+1 as the
    // population grows. The deadline is a bound parameter, never SQL NOW(), for the same reason the
    // domain compares instants in Java (see MfaGrace).
    return entityManager.createQuery(
            "SELECT g FROM MfaGrace g WHERE g.graceUntil > :now "
                + "AND (g.reminderDueSentAt IS NULL OR g.reminderFinalSentAt IS NULL)",
            MfaGrace.class)
        .setParameter("now", now)
        .getResultList();
  }

  @Override
  public void markDueReminderSent(String userId, java.time.Instant now) {
    // "AND ... IS NULL" makes the write idempotent as well: two instances racing on the same window
    // cannot overwrite the first stamp, so the record says when it was first sent.
    entityManager.createQuery("UPDATE MfaGrace g SET g.reminderDueSentAt = :now "
            + "WHERE g.userId = :userId AND g.reminderDueSentAt IS NULL")
        .setParameter("now", now)
        .setParameter("userId", userId)
        .executeUpdate();
  }

  @Override
  public void markFinalReminderSent(String userId, java.time.Instant now) {
    entityManager.createQuery("UPDATE MfaGrace g SET g.reminderFinalSentAt = :now "
            + "WHERE g.userId = :userId AND g.reminderFinalSentAt IS NULL")
        .setParameter("now", now)
        .setParameter("userId", userId)
        .executeUpdate();
  }
}
