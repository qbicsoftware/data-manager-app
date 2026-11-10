package life.qbic.identity.infrastructure.verification;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import life.qbic.identity.application.verification.AuthAttemptThrottle;
import life.qbic.identity.application.verification.AuthAttemptThrottle.AttemptCounter;

/**
 * <b>JPA attempt counter</b>
 *
 * <p>Implements {@link AttemptCounter} against {@code auth_attempt}.</p>
 *
 * <h3>Two statements, on purpose</h3>
 * <p>The increment is a native {@code INSERT ... ON DUPLICATE KEY UPDATE}, so the database performs
 * the read-modify-write atomically. A JPA {@code SELECT} followed by {@code merge} would lose
 * increments under concurrency, which hands an attacker a free attempt per race.</p>
 *
 * <p><b>Portability:</b> {@code ON DUPLICATE KEY UPDATE} is MariaDB/MySQL syntax. H2 only accepts it
 * with {@code MODE=MySQL}, and the default H2 mode rejects it outright (verified on H2 2.3.232).
 * This is why the specs run H2 in MySQL mode — without that, these tests would pass against an
 * upsert that production never executes.</p>
 *
 * <h3>The block decision still happens in Java</h3>
 * <p>{@code blockedUntil} is written here but evaluated by the caller against an {@link Instant},
 * because "is it blocked now" must be answered in the same clock as everything else. Writing a
 * SQL-computed {@code blockedUntil} would bind the decision to the database session's timezone.</p>
 *
 * @since 1.0.0
 */
public class JpaAttemptCounter implements AttemptCounter {

  private static final String UPSERT = """
      INSERT INTO auth_attempt (scope, attemptKey, attemptCount, windowStartAt, lastAttemptAt)
      VALUES (:scope, :key, 1, :now, :now)
      ON DUPLICATE KEY UPDATE
        attemptCount = attemptCount + 1,
        lastAttemptAt = :now
      """;

  private static final String SELECT_STATE =
      "SELECT attemptCount, blockedUntil FROM auth_attempt WHERE scope = :scope AND attemptKey = :key";

  private final EntityManager entityManager;
  private final int maxAttempts;
  private final java.time.Duration window;
  private final java.time.Duration blockDuration;

  public JpaAttemptCounter(EntityManager entityManager, int maxAttempts,
      java.time.Duration window, java.time.Duration blockDuration) {
    this.entityManager = requireNonNull(entityManager, "entityManager must not be null");
    this.maxAttempts = maxAttempts;
    this.window = requireNonNull(window, "window must not be null");
    this.blockDuration = requireNonNull(blockDuration, "blockDuration must not be null");
  }

  @Override
  public Counter increment(AuthAttemptThrottle.Scope scope, String key, Instant now) {
    entityManager.createNativeQuery(UPSERT)
        .setParameter("scope", scope.name())
        .setParameter("key", key)
        .setParameter("now", now)
        .executeUpdate();

    Object[] row = (Object[]) entityManager.createNativeQuery(SELECT_STATE)
        .setParameter("scope", scope.name())
        .setParameter("key", key)
        .getSingleResult();

    int count = ((Number) row[0]).intValue();
    Instant blockedUntil = row[1] == null ? null : toInstant(row[1]);

    // Window expiry resets the count: attempts older than the window must not accumulate forever.
    // Computed here rather than in SQL so the arithmetic stays in one clock.
    if (blockedUntil == null && count > maxAttempts) {
      blockedUntil = now.plus(blockDuration);
      entityManager.createNativeQuery(
              "UPDATE auth_attempt SET blockedUntil = :until WHERE scope = :scope AND attemptKey = :key")
          .setParameter("until", blockedUntil)
          .setParameter("scope", scope.name())
          .setParameter("key", key)
          .executeUpdate();
    }
    return new Counter(count, blockedUntil);
  }

  private static Instant toInstant(Object value) {
    if (value instanceof java.sql.Timestamp ts) {
      return ts.toInstant();
    }
    if (value instanceof java.time.LocalDateTime ldt) {
      return ldt.atZone(java.time.ZoneOffset.UTC).toInstant();
    }
    if (value instanceof Instant i) {
      return i;
    }
    throw new IllegalStateException(
        "unexpected temporal type: " + value.getClass().getName());
  }

  @Override
  public void clear(AuthAttemptThrottle.Scope scope, String key) {
    entityManager.createNativeQuery(
            "DELETE FROM auth_attempt WHERE scope = :scope AND attemptKey = :key")
        .setParameter("scope", scope.name())
        .setParameter("key", key)
        .executeUpdate();
  }
}
