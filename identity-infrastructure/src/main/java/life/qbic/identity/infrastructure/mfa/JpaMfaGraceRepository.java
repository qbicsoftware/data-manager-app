package life.qbic.identity.infrastructure.mfa;

import static life.qbic.logging.service.LoggerFactory.logger;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import life.qbic.identity.domain.model.mfa.MfaGrace;
import life.qbic.identity.domain.repository.MfaGraceRepository;
import life.qbic.logging.api.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
 * <h3>Why the two reminder stamps declare their own transaction</h3>
 *
 * <p>{@code markDueReminderSent} and {@code markFinalReminderSent} are JPQL bulk updates, and
 * Hibernate refuses a bulk update without an active transaction
 * ({@code TransactionRequiredException}). Nothing supplied one: the scheduled
 * {@code MfaReminderService.sendDueReminders} carries no {@code @Transactional} — deliberately, see
 * below — and the application runs with {@code spring.jpa.open-in-view=false}.</p>
 *
 * <p>So on a deployed instance in {@code REQUIRED} mode the pass <em>selected</em> the window (a read
 * needs no transaction), <em>sent</em> the mail, and then threw when it tried to record that it had.
 * The reminder job logs and swallows that, so the failure was invisible from the outside: one mail
 * every five minutes, and no record of any of them. Reported as "swamped with reminder emails".</p>
 *
 * <p><b>{@code REQUIRES_NEW}, not {@code REQUIRED}.</b> The service passes over many accounts and
 * must not hold a transaction across them, so it declares none. If these methods joined the caller's
 * transaction — or started one that a later failure in the same pass rolled back — the stamps of
 * accounts already mailed would be undone and everybody would be mailed again. The interface's
 * contract says a stamp is never rolled back, and an independent commit is the only way to keep
 * that. Each method also carries its own {@code AND ... IS NULL} guard, so two instances racing on
 * one window cannot overwrite the first stamp.</p>
 *
 * <p>Marked on the adapter rather than the service for the same reason {@code AuthAttemptThrottle}
 * marks its write paths: the control belongs where the transaction-requiring call is, so a future
 * caller cannot forget it.</p>
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

    // ── The insert itself is the answer, so no read is needed to know what happened ────────────
    //
    // MariaDB/MySQL report 1 row affected when the INSERT inserted, and 0 when ON DUPLICATE KEY
    // found an existing row and updated nothing ("userId = userId" is a no-op). Connector/J sends
    // CLIENT_FOUND_ROWS off, so a no-op update really does report 0.
    //
    // This is not a stylistic preference. The read-back that used to run here was the bug: at
    // REPEATABLE READ (MariaDB's default) a plain SELECT keeps reading the snapshot taken at the
    // transaction's first read, so a row committed by a concurrent login stays invisible forever —
    // the insert returns 0, the row exists, and the SELECT reports nothing. Observed on a deployed
    // instance as an HTTP 500 on every login of one account.
    //
    // When the insert is authoritative, this is exact:
    if (affected == 1) {
      return candidate;
    }

    // affected == 0: a row existed when this statement ran. Somebody else's committed row is now the
    // truth, and its deadline must not be moved by this login (grace is not renewable).
    List<MfaGrace> stored = entityManager
        .createQuery("SELECT g FROM MfaGrace g WHERE g.userId = :userId", MfaGrace.class)
        .setParameter("userId", candidate.userId())
        // PESSIMISTIC_WRITE for the *read of somebody else's row*, which is the case where the
        // snapshot can be stale. In InnoDB a locking read is a current read in every isolation level,
        // so it sees the row the duplicate check just saw.
        .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
        .getResultList();
    if (!stored.isEmpty()) {
      return stored.getFirst();
    }

    // Still nothing. Whatever the cause, this value is never read by a caller — the login guard
    // discards it — so denying the login over it would be the wrong trade: the session is legitimate
    // and only a bookkeeping figure is missing.
    log.error("Grace window for user %s reported an existing row (affected rows: 0) but no row can be "
        .formatted(candidate.userId())
        + "read back; proceeding with the candidate until %s. %s"
        .formatted(candidate.graceUntil(), describeSchema()));
    return candidate;
  }

  /**
   * Names the schema the connection is actually writing to, and how many rows a native query sees.
   *
   * <p>Two statements, two different views of the same table, and one of them writes: the only
   * explanations left are that they are not the same table. Recording the effective schema turns that
   * from a guess into a fact in the log, which is what a report of "not readable after insert" lacked.</p>
   */
  private String describeSchema() {
    return "Effective schema: " + probe("SELECT DATABASE()")
        + "; autocommit: " + probe("SELECT @@autocommit")
        + "; isolation: " + probe("SELECT @@transaction_isolation")
        + "; connection: " + probe("SELECT CONNECTION_ID()")
        + ". A row that committed but reads as absent means the readers and the writer are not the "
        + "same table or not the same transaction. Check the schema above, and check that the "
        + "migration was applied to it.";
  }

  /**
   * One value from the database, as text, never throwing.
   *
   * <p>Diagnostics must not turn into a second failure: an exception here would replace the observation
   * that matters with a different one.</p>
   */
  private String probe(String sql) {
    try {
      return String.valueOf(entityManager.createNativeQuery(sql).getSingleResult());
    } catch (RuntimeException e) {
      return "unknown (" + e.getClass().getSimpleName() + ")";
    }
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
  @Transactional(propagation = Propagation.REQUIRES_NEW)
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
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFinalReminderSent(String userId, java.time.Instant now) {
    entityManager.createQuery("UPDATE MfaGrace g SET g.reminderFinalSentAt = :now "
            + "WHERE g.userId = :userId AND g.reminderFinalSentAt IS NULL")
        .setParameter("now", now)
        .setParameter("userId", userId)
        .executeUpdate();
  }
}
