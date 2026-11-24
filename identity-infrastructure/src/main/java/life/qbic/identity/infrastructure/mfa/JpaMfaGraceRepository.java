package life.qbic.identity.infrastructure.mfa;

import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.MfaGrace;
import life.qbic.identity.domain.repository.MfaGraceRepository;
import life.qbic.logging.api.Logger;
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

  private static final Logger log = logger(JpaMfaGraceRepository.class);

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
    int affected = entityManager.createNativeQuery("""
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
      // A native re-read before giving up: if the ORM-level query misses a row the native one can see,
      // the problem is in the mapping or the query, and the log must say which. If both miss it, the
      // row is not there for this connection, which is a different diagnosis again.
      Optional<MfaGrace> nativeRow = readBackNatively(candidate.userId());
      if (nativeRow.isPresent()) {
        log.warn("Grace window for user %s was invisible to the ORM query but readable natively; "
            .formatted(candidate.userId())
            + "returning the stored row. The entity mapping and the table disagree.");
        return nativeRow.get();
      }
      // Do NOT throw. This read is an assertion, and no caller reads its result — the login guard
      // discards it. Throwing here turned one unreadable row into a failed login (observed on a
      // deployed instance: a 500 for every attempt), which is the worst possible trade: the write
      // succeeded, the session is legitimate, and the account is denied because a bookkeeping read
      // came back empty.
      //
      // The cost of returning the candidate is bounded: at worst this login proceeds under a deadline
      // one window later than the stored one. The stored row is untouched and first-write-wins still
      // holds, so the next login reads the real deadline again.
      log.error("Grace window for user %s was written (affected rows: %d) but is not readable in the "
          .formatted(candidate.userId(), affected)
          + "same transaction, by ORM or by native query; proceeding with the candidate until %s. %s"
          .formatted(candidate.graceUntil(), describeSchema()));
      return candidate;
    }
    return stored.getFirst();
  }

  /**
   * The same row, read without Hibernate's mapping in the way.
   *
   * <p>Deliberately minimal: three columns, no entity, no query plan. It exists to separate "the row is
   * not there" from "our query does not see it", which are different problems with different fixes.</p>
   */
  private Optional<MfaGrace> readBackNatively(String userId) {
    try {
      Object[] row = (Object[]) entityManager
          .createNativeQuery("SELECT userId, startedAt, graceUntil FROM user_mfa_grace "
              + "WHERE userId = :userId")
          .setParameter("userId", userId)
          .getResultList().stream().findFirst().orElse(null);
      if (row == null) {
        return Optional.empty();
      }
      return Optional.of(MfaGrace.start(String.valueOf(row[0]),
          toInstant(row[1]),
          java.time.Duration.between(toInstant(row[1]), toInstant(row[2]))));
    } catch (RuntimeException e) {
      // Reported by the caller's log line; a failure here must not mask the original observation.
      return Optional.empty();
    }
  }

  private static java.time.Instant toInstant(Object value) {
    return switch (value) {
      case java.time.Instant instant -> instant;
      case java.sql.Timestamp timestamp -> timestamp.toInstant();
      case java.time.LocalDateTime local -> local.toInstant(java.time.ZoneOffset.UTC);
      case String text -> java.time.Instant.parse(text.replace(' ', 'T') + "Z");
      case null, default -> throw new IllegalStateException(
          "unexpected column type " + (value == null ? "null" : value.getClass().getName()));
    };
  }

  /**
   * Names the schema the connection is actually writing to, and how many rows a native query sees.
   *
   * <p>Two statements, two different views of the same table, and one of them writes: the only
   * explanations left are that they are not the same table. Recording the effective schema turns that
   * from a guess into a fact in the log, which is what a report of "not readable after insert" lacked.</p>
   */
  private String describeSchema() {
    String schema;
    try {
      schema = String.valueOf(entityManager
          .createNativeQuery("SELECT DATABASE()")
          .getSingleResult());
    } catch (RuntimeException e) {
      schema = "unknown (" + e.getClass().getSimpleName() + ")";
    }
    return "Effective schema: " + schema + "; check that the migration was applied to it and that "
        + "no trigger or view intercepts user_mfa_grace writes.";
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
