package life.qbic.identity.infrastructure.throttle;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import life.qbic.identity.domain.repository.AuthAttemptCounter;
import org.springframework.stereotype.Repository;

/**
 * <b>JPA attempt counter</b>
 *
 * <p>Implements {@link AuthAttemptCounter} against {@code auth_attempt} (see
 * {@code sql/migrations/auth-attempt-throttling.sql}).</p>
 *
 * <h3>The increment is a native upsert, on purpose</h3>
 * <p>{@code INSERT … ON DUPLICATE KEY UPDATE attemptCount = attemptCount + 1} lets the database
 * perform the read-modify-write atomically. A JPA {@code SELECT} followed by {@code merge} loses
 * increments under concurrency — two attempts both read {@code 3} and both write {@code 4} — which
 * hands an attacker a free attempt per race.</p>
 *
 * <h3>Portability: MariaDB/MySQL syntax, and the specs must say so</h3>
 * <p>{@code ON DUPLICATE KEY UPDATE} is MariaDB/MySQL. H2 accepts it only with {@code MODE=MySQL};
 * default H2 rejects it outright. The throttling specs therefore run H2 in MySQL mode, or they would
 * pass against an upsert that production never executes.</p>
 *
 * <h3>Time is written and compared in UTC, never via NOW()</h3>
 * <p>Every column is {@code datetime(6)} with no timezone. Passing a {@link LocalDateTime} derived
 * from an {@link Instant} at {@link ZoneOffset#UTC} keeps the stored value in one clock; letting
 * MySQL supply {@code NOW()} would use the session zone and disagree with the application by the
 * offset.</p>
 *
 * @since 1.0.0
 */
@Repository
public class JpaAuthAttemptCounter implements AuthAttemptCounter {

  /**
   * A single statement: insert the first attempt, or advance the existing row — including rolling
   * the window over.
   *
   * <h3>Why the window reset is folded in here and not a separate UPDATE</h3>
   * <p>The obvious implementation is {@code UPDATE … WHERE windowStartAt < ?} followed by the
   * {@code INSERT … ON DUPLICATE KEY UPDATE}. That deadlocks on real InnoDB: the UPDATE takes locks
   * across the unique-key range, then the INSERT needs the insert-intention lock on the same range,
   * and 16 concurrent attempts on one bucket produce
   * {@code Deadlock found when trying to get lock}. It is the same failure the verification-token
   * reissue hit (see {@code docs/plans/spike-verification-token-flow.md}, Finding 6), and it did not
   * reproduce on H2 — only on MariaDB.</p>
   *
   * <p>Rolling the window over inside the upsert keeps it to one statement, so there is no second
   * lock acquisition and no ordering to get wrong.</p>
   *
   * <p><b>Assignment order is load-bearing.</b> MySQL/MariaDB evaluate
   * {@code ON DUPLICATE KEY UPDATE} assignments left to right, and later assignments see the values
   * already written by earlier ones. {@code attemptCount} and {@code blockedUntil} therefore read
   * the <em>old</em> {@code windowStartAt}, and {@code windowStartAt} is assigned last.</p>
   *
   * <p><b>{@code CASE WHEN}, not {@code IF()}.</b> MariaDB accepts both, but H2 in {@code MODE=MySQL}
   * rejects {@code IF()} inside {@code ON DUPLICATE KEY UPDATE} with a syntax error — so the H2 run
   * would fail even though production is fine, and the in-memory suite is the fast feedback loop.
   * {@code CASE WHEN} is standard and both engines accept it.</p>
   */
  private static final String UPSERT = """
      INSERT INTO auth_attempt (scope, attemptKey, attemptCount, windowStartAt, lastAttemptAt)
      VALUES (:scope, :key, 1, :windowStart, :now)
      ON DUPLICATE KEY UPDATE
        attemptCount  = CASE WHEN windowStartAt < :windowStart THEN 1 ELSE attemptCount + 1 END,
        blockedUntil  = CASE WHEN windowStartAt < :windowStart THEN NULL ELSE blockedUntil END,
        lastAttemptAt = :now,
        windowStartAt = CASE WHEN windowStartAt < :windowStart THEN :windowStart ELSE windowStartAt END
      """;

  private static final String SELECT_STATE =
      "SELECT attemptCount, blockedUntil, windowStartAt FROM auth_attempt"
          + " WHERE scope = :scope AND attemptKey = :key";

  private final EntityManager entityManager;

  /**
   * The shared, container-managed {@link EntityManager}.
   *
   * <p>Deliberately not self-created: the increment and the block have to see the caller's
   * transaction, and a private EntityManager would open its own and break that.</p>
   */
  public JpaAuthAttemptCounter(EntityManager entityManager) {
    this.entityManager = requireNonNull(entityManager, "entityManager must not be null");
  }

  @Override
  public Counter increment(String scope, String key, Instant now, Instant windowStart) {
    requireNonNull(scope, "scope must not be null");
    requireNonNull(key, "key must not be null");
    requireNonNull(now, "now must not be null");
    requireNonNull(windowStart, "windowStart must not be null");

    // ONE statement. See UPSERT's javadoc: splitting the window roll-over into a preceding UPDATE
    // deadlocks on InnoDB, and the deadlock does not reproduce on H2.
    entityManager.createNativeQuery(UPSERT)
        .setParameter("scope", scope)
        .setParameter("key", key)
        .setParameter("windowStart", toUtc(windowStart))
        .setParameter("now", toUtc(now))
        .executeUpdate();

    return currentState(scope, key);
  }

  @Override
  public void block(String scope, String key, Instant blockedUntil) {
    requireNonNull(blockedUntil, "blockedUntil must not be null");
    entityManager.createNativeQuery("""
            UPDATE auth_attempt
               SET blockedUntil = :blockedUntil
             WHERE scope = :scope AND attemptKey = :key
            """)
        .setParameter("blockedUntil", toUtc(blockedUntil))
        .setParameter("scope", scope)
        .setParameter("key", key)
        .executeUpdate();
  }

  @Override
  public void clear(String scope, String key) {
    entityManager.createNativeQuery(
            "DELETE FROM auth_attempt WHERE scope = :scope AND attemptKey = :key")
        .setParameter("scope", scope)
        .setParameter("key", key)
        .executeUpdate();
  }

  private Counter currentState(String scope, String key) {
    Object[] row = (Object[]) entityManager.createNativeQuery(SELECT_STATE)
        .setParameter("scope", scope)
        .setParameter("key", key)
        .getSingleResult();
    return new Counter(((Number) row[0]).intValue(), row[1] == null ? null : toInstant(row[1]));
  }

  @Override
  public Counter peek(String scope, String key) {
    // No write. A throttled request is not an attempt, and making the check write would let a
    // blocked client extend its own block simply by retrying.
    var rows = entityManager.createNativeQuery(SELECT_STATE)
        .setParameter("scope", scope)
        .setParameter("key", key)
        .getResultList();
    if (rows.isEmpty()) {
      return new Counter(0, null);
    }
    Object[] row = (Object[]) rows.get(0);
    return new Counter(((Number) row[0]).intValue(), row[1] == null ? null : toInstant(row[1]));
  }

  /**
   * Converts to the {@code datetime(6)}-friendly form the driver expects.
   *
   * <p>Written in UTC so the stored value is unambiguous. The inverse, {@link #toInstant}, assumes
   * the same, which is why {@code NOW()} must never write these columns.</p>
   */
  private static LocalDateTime toUtc(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static Instant toInstant(Object value) {
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.toInstant(ZoneOffset.UTC);
    }
    if (value instanceof java.time.OffsetDateTime offsetDateTime) {
      return offsetDateTime.toInstant();
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant();
    }
    throw new IllegalStateException(
        "unexpected temporal type: " + value.getClass().getName());
  }

  /**
   * Deletes counters whose last attempt is older than {@code cutoff}.
   *
   * <p>Deliberately not scheduled yet: retention is a compliance decision owned by #1669, and this
   * table holds no audit trail worth preserving, so leaving it alone risks only unbounded growth.
   * Exposed so the purge job can call it once that decision is recorded, rather than the job
   * re-implementing the predicate.</p>
   *
   * @param cutoff rows whose {@code lastAttemptAt} is before this instant are removed
   * @return the number of rows removed
   */
  public int purgeOlderThan(Instant cutoff) {
    return entityManager.createNativeQuery(
            "DELETE FROM auth_attempt WHERE lastAttemptAt < :cutoff")
        .setParameter("cutoff", toUtc(cutoff))
        .executeUpdate();
  }
}
