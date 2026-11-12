package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.domain.repository.AuthAttemptCounter
import life.qbic.identity.infrastructure.throttle.JpaAuthAttemptCounter
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * <b>The throttle counter must never lose an increment (AUTH-NFR-03, #1682).</b>
 *
 * <p>This is the one property that cannot be reviewed from the schema and cannot be tested with a
 * single-threaded spec. Read-then-write loses increments: two concurrent attempts both read
 * {@code attemptCount = 3} and both write {@code 4}, so an attacker gains a free attempt per race.
 * N concurrent attempts must therefore leave the counter at exactly N — not at N minus the number
 * of races.</p>
 *
 * <p>The implementation uses {@code INSERT … ON DUPLICATE KEY UPDATE}, which the database executes
 * atomically. That is MariaDB/MySQL syntax, accepted by H2 only in {@code MODE=MySQL} — which is why
 * the in-memory default below sets it. Without that, these specs would pass against an upsert
 * production never executes.</p>
 *
 * <p>Run against real MariaDB with {@code ./scripts/run-verification-spike.sh mariadb}: InnoDB's
 * upsert semantics are the ones that matter, and H2's are not a substitute.</p>
 */
class AuthAttemptCounterConcurrencySpec extends Specification {

  @Shared
  EntityManagerFactory emf

  boolean schemaReady

  static final Instant T0 = Instant.parse('2026-11-11T10:00:00Z')
  static final String ADDRESS = 'alice@example.org'
  static final String SCOPE = 'LOGIN'

  // ---------------------------------------------------------------- database under test

  static String jdbcUrl() {
    System.getProperty('verification.jdbc.url',
        'jdbc:h2:mem:auththrottle;MODE=MySQL;DB_CLOSE_DELAY=-1')
  }

  static String jdbcUser() {
    System.getProperty('verification.jdbc.user', 'sa')
  }

  static String jdbcPassword() {
    System.getProperty('verification.jdbc.password', '')
  }

  static String jdbcDriver() {
    System.getProperty('verification.jdbc.driver', 'org.h2.Driver')
  }

  static String dialect() {
    System.getProperty('verification.dialect')
  }

  def setupSpec() {
    def builder = new StandardServiceRegistryBuilder()
        .applySetting(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl())
        .applySetting(AvailableSettings.JAKARTA_JDBC_USER, jdbcUser())
        .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, jdbcPassword())
        .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, jdbcDriver())
    if (dialect() != null) {
      builder.applySetting(AvailableSettings.DIALECT, dialect())
    }
    def registry = builder.build()
    // No annotated classes: the counter is native SQL, so there is no @Entity to map. The table is
    // created from the migration script's shape in setup() instead of derived by Hibernate.
    emf = new MetadataSources(registry).buildMetadata().buildSessionFactory()
  }

  def cleanupSpec() {
    emf?.close()
  }

  def setup() {
    // The table is created from the real migration script's shape, not from Hibernate: the entity
    // mapping is native SQL, so there is no @Entity for Hibernate to derive a table from.
    inTransaction { EntityManager em ->
      em.createNativeQuery('''CREATE TABLE IF NOT EXISTS auth_attempt (
          id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
          scope varchar(32) NOT NULL,
          attemptKey char(64) NOT NULL,
          attemptCount int NOT NULL DEFAULT 0,
          windowStartAt datetime(6) NOT NULL,
          windowSeconds int NOT NULL DEFAULT 900,
          blockedUntil datetime(6) DEFAULT NULL,
          lastAttemptAt datetime(6) NOT NULL,
          UNIQUE KEY uk_auth_attempt_scope_key (scope, attemptKey)
        )''').executeUpdate()
    }
    inTransaction { EntityManager em ->
      em.createNativeQuery('DELETE FROM auth_attempt').executeUpdate()
    }
    schemaReady = true
  }

  // ------------------------------------------- the bug that shipped: window never accumulated

  def "successive attempts at different instants ACCUMULATE — the count must rise 1,2,3,..."() {
    // This is the regression guard for a bug that reached a running server.
    //
    // The first implementation asked the caller for `windowStart = now - window` and reset the row
    // whenever the stored start was earlier than that. Because every call computes a LATER boundary,
    // the stored start was ALWAYS earlier, so every attempt was treated as a fresh window and reset
    // the count. Measured: [1, 1, 1, 1, 1]. The threshold was therefore never reached and the throttle
    // never engaged — the login flow silently had no rate limit at all.
    //
    // Every other case in this spec used a CONSTANT instant for `now`, which hid it completely.
    given:
    def counter = newThrottle(3)

    when: 'five attempts, each 10 seconds after the previous'
    def decisions = (0..4).collect { i ->
      counter.record(AuthAttemptThrottle.Scope.LOGIN, ADDRESS, T0.plusSeconds(i * 10L))
    }

    then: 'the first three are allowed and the rest are throttled — the threshold was actually reached'
    decisions == [
        AuthAttemptThrottle.Decision.ALLOWED,
        AuthAttemptThrottle.Decision.ALLOWED,
        AuthAttemptThrottle.Decision.ALLOWED,
        AuthAttemptThrottle.Decision.THROTTLED,
        AuthAttemptThrottle.Decision.THROTTLED,
    ]

    and: 'the persisted count reflects the real number of attempts, not 1'
    storedCount(SCOPE, AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)) == 5
  }

  def "the count resets only once the row's own window has actually elapsed"() {
    given: 'a 15 minute window'
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)

    when: 'three attempts spread over 30 seconds'
    inTransaction { EntityManager em ->
      def counter = new JpaAuthAttemptCounter(em)
      counter.increment(SCOPE, key, T0, Duration.ofMinutes(15))
      counter.increment(SCOPE, key, T0.plusSeconds(15), Duration.ofMinutes(15))
      counter.increment(SCOPE, key, T0.plusSeconds(30), Duration.ofMinutes(15))
    }

    then: 'they all belong to one window'
    storedCount(SCOPE, key) == 3

    when: 'an attempt arrives one second past the window end'
    def later = T0.plus(Duration.ofMinutes(15)).plusSeconds(1)
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).increment(SCOPE, key, later, Duration.ofMinutes(15))
    }

    then: 'the window rolled over and the count restarted at 1'
    storedCount(SCOPE, key) == 1
  }

  def "an elapsed window clears a stale block, so an expired block cannot outlive its window"() {
    given:
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)
    inTransaction { EntityManager em ->
      def counter = new JpaAuthAttemptCounter(em)
      counter.increment(SCOPE, key, T0, Duration.ofMinutes(15))
      counter.block(SCOPE, key, T0.plus(Duration.ofMinutes(15)))
    }

    when:
    def later = T0.plus(Duration.ofMinutes(20))
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).increment(SCOPE, key, later, Duration.ofMinutes(15))
    }

    then:
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).peek(SCOPE, key).blockedUntil() == null
    }
  }

  // --------------------------------------------------------------- the property that matters

  def "N concurrent attempts leave the counter at exactly N"() {
    // Read-then-write would land below N. The size of the gap is the number of races, so an attacker
    // gets exactly that many free password guesses.
    given:
    int threads = 16
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)
    def start = new CountDownLatch(1)
    def pool = Executors.newFixedThreadPool(threads)

    when: 'all threads increment the same bucket at the same moment'
    def futures = (1..threads).collect {
      pool.submit({
        start.await()
        inTransaction { EntityManager em ->
          new JpaAuthAttemptCounter(em).increment(SCOPE, key, T0, Duration.ofMinutes(15))
        }
      } as Callable)
    }
    start.countDown()
    futures.each { it.get(60, TimeUnit.SECONDS) }
    pool.shutdown()

    then: 'no increment was lost — this is what an upsert buys and read-then-write does not'
    storedCount(SCOPE, key) == threads
  }

  def "N concurrent attempts produce exactly one row, never duplicates"() {
    given:
    int threads = 16
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)
    def start = new CountDownLatch(1)
    def pool = Executors.newFixedThreadPool(threads)

    when:
    def futures = (1..threads).collect {
      pool.submit({
        start.await()
        inTransaction { EntityManager em ->
          new JpaAuthAttemptCounter(em).increment(SCOPE, key, T0, Duration.ofMinutes(15))
        }
      } as Callable)
    }
    start.countDown()
    futures.each { it.get(60, TimeUnit.SECONDS) }
    pool.shutdown()

    then: 'the UNIQUE KEY is what makes the upsert atomic; duplicates would split the count silently'
    storedRowCount(SCOPE, key) == 1
  }

  def "the threshold is still reached under concurrency, so the block actually engages"() {
    // The end-to-end version of the property above: if increments were lost, a burst would slip under
    // the threshold and the throttle would never arm.
    given: 'a limit of 10, hit by 16 concurrent attempts'
    def throttle = newThrottle(10)
    int threads = 16
    def start = new CountDownLatch(1)
    def pool = Executors.newFixedThreadPool(threads)

    when:
    def futures = (1..threads).collect {
      pool.submit({
        start.await()
        inTransaction { EntityManager em ->
          new AuthAttemptThrottle(new JpaAuthAttemptCounter(em),
              10, Duration.ofMinutes(15), Duration.ofMinutes(15),
              3, Duration.ofMinutes(15), Duration.ofMinutes(15))
              .record(AuthAttemptThrottle.Scope.LOGIN, ADDRESS, T0)
        }
      } as Callable)
    }
    start.countDown()
    futures.each { it.get(60, TimeUnit.SECONDS) }
    pool.shutdown()

    then: 'the bucket went over the limit and the address is now blocked'
    throttle.isThrottled(AuthAttemptThrottle.Scope.LOGIN, ADDRESS, T0.plusSeconds(30))
  }

  // --------------------------------------------------------------- window and block handling

  def "an elapsed window restarts the count instead of accumulating forever"() {
    // Without this, a user whose occasional typos are spread over months is eventually blocked for
    // behaviour that never looked like an attack.
    given:
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)
    inTransaction { EntityManager em ->
      def counter = new JpaAuthAttemptCounter(em)
      counter.increment(SCOPE, key, T0, Duration.ofMinutes(15))
      counter.increment(SCOPE, key, T0.plusSeconds(1), Duration.ofMinutes(15))
    }
    assert storedCount(SCOPE, key) == 2

    when: 'an attempt arrives well after the window closed'
    def later = T0.plus(Duration.ofHours(2))
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em)
          .increment(SCOPE, key, later, Duration.ofMinutes(15))
    }

    then: 'the window restarted, so the count is 1 rather than 3'
    storedCount(SCOPE, key) == 1
  }

  def "a stored block is readable and is cleared when its window rolls over"() {
    given:
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)
    def blockedUntil = T0.plus(Duration.ofMinutes(15))

    when:
    inTransaction { EntityManager em ->
      def counter = new JpaAuthAttemptCounter(em)
      counter.increment(SCOPE, key, T0, Duration.ofMinutes(15))
      counter.block(SCOPE, key, blockedUntil)
    }

    then: 'the block is visible without incrementing'
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).peek(SCOPE, key).blockedUntil() == blockedUntil
    }

    when: 'the window later rolls over'
    def later = T0.plus(Duration.ofHours(3))
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).increment(SCOPE, key, later, Duration.ofMinutes(15))
    }

    then: 'the stale block is gone, so an expired block cannot outlive its window'
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).peek(SCOPE, key).blockedUntil() == null
    }
  }

  def "peek never writes, so a rejected request cannot extend its own block"() {
    given: 'a blocked bucket'
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)
    inTransaction { EntityManager em ->
      def counter = new JpaAuthAttemptCounter(em)
      counter.increment(SCOPE, key, T0, Duration.ofMinutes(15))
      counter.block(SCOPE, key, T0.plus(Duration.ofMinutes(15)))
    }
    def countBefore = storedCount(SCOPE, key)

    when: 'a blocked client retries repeatedly'
    20.times {
      inTransaction { EntityManager em ->
        new JpaAuthAttemptCounter(em).peek(SCOPE, key)
      }
    }

    then: 'the counter did not move — otherwise retrying would extend the block forever'
    storedCount(SCOPE, key) == countBefore
  }

  def "clear removes the bucket so the next window starts clean"() {
    given:
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).increment(SCOPE, key, T0, Duration.ofMinutes(15))
    }

    when:
    inTransaction { EntityManager em -> new JpaAuthAttemptCounter(em).clear(SCOPE, key) }

    then:
    storedRowCount(SCOPE, key) == 0
  }

  def "the purge only removes rows older than the cutoff"() {
    given:
    def freshKey = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, 'fresh@example.org')
    def staleKey = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, 'stale@example.org')
    inTransaction { EntityManager em ->
      def counter = new JpaAuthAttemptCounter(em)
      counter.increment(SCOPE, freshKey, T0, Duration.ofMinutes(15))
      counter.increment(SCOPE, staleKey, T0.minus(Duration.ofDays(30)), Duration.ofMinutes(15))
    }

    when:
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).purgeOlderThan(T0.minus(Duration.ofDays(7)))
    }

    then:
    storedRowCount(SCOPE, freshKey) == 1
    storedRowCount(SCOPE, staleKey) == 0
  }

  def "peek on a key that was never attempted returns an empty, unblocked counter"() {
    // The pre-request check runs on every login, including the first one for an address, so this is
    // the most-executed path in the whole throttle. Returning null here would NPE on every first
    // login attempt.
    given:
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, 'never-seen@example.org')

    expect:
    inTransaction { EntityManager em ->
      def state = new JpaAuthAttemptCounter(em).peek(SCOPE, key)
      state.attemptCount() == 0 && state.blockedUntil() == null
    }
  }

  def "a stored blockedUntil is read back as the same instant it was written"() {
    // Guards the UTC round-trip. Every column is datetime(6) with no zone, so a write in one zone and
    // a read in another would shift the block by the offset — and a block that ends early is not a
    // block.
    given:
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)
    def blockedUntil = Instant.parse('2026-11-11T12:34:56.123456Z')

    when:
    inTransaction { EntityManager em ->
      def counter = new JpaAuthAttemptCounter(em)
      counter.increment(SCOPE, key, T0, Duration.ofMinutes(15))
      counter.block(SCOPE, key, blockedUntil)
    }

    then:
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).peek(SCOPE, key).blockedUntil() == blockedUntil
    }
  }

  def "purging an empty table is a no-op rather than an error"() {
    expect:
    inTransaction { EntityManager em ->
      new JpaAuthAttemptCounter(em).purgeOlderThan(T0) == 0
    }
  }

  // --------------------------------------------------------------- helpers

  private AuthAttemptThrottle newThrottle(int maxAttempts) {
    new AuthAttemptThrottle(proxyCounter(), maxAttempts, Duration.ofMinutes(15),
        Duration.ofMinutes(15), 3, Duration.ofMinutes(15), Duration.ofMinutes(15))
  }

  /** A counter that opens its own transaction per call, so the throttle can be used outside one. */
  private AuthAttemptCounter proxyCounter() {
    return new PerCallCounter(this)
  }

  static final class PerCallCounter implements AuthAttemptCounter {

    private final AuthAttemptCounterConcurrencySpec spec

    PerCallCounter(AuthAttemptCounterConcurrencySpec spec) {
      this.spec = spec
    }

    @Override
    AuthAttemptCounter.Counter increment(String scope, String key, Instant now, Duration window) {
      spec.inTransaction { EntityManager em ->
        new JpaAuthAttemptCounter(em).increment(scope, key, now, window)
      }
    }

    @Override
    AuthAttemptCounter.Counter peek(String scope, String key) {
      spec.inTransaction { EntityManager em -> new JpaAuthAttemptCounter(em).peek(scope, key) }
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
      spec.inTransaction { EntityManager em ->
        new JpaAuthAttemptCounter(em).block(scope, key, blockedUntil)
      }
    }

    @Override
    void clear(String scope, String key) {
      spec.inTransaction { EntityManager em -> new JpaAuthAttemptCounter(em).clear(scope, key) }
    }
  }

  private def inTransaction(Closure<?> work) {
    EntityManager em = emf.createEntityManager()
    try {
      em.transaction.begin()
      def result = work.call(em)
      em.transaction.commit()
      return result
    } catch (Exception e) {
      if (em.transaction.active) {
        em.transaction.rollback()
      }
      throw e
    } finally {
      em.close()
    }
  }

  private int storedCount(String scope, String key) {
    inTransaction { EntityManager em ->
      def rows = em.createNativeQuery(
          'SELECT attemptCount FROM auth_attempt WHERE scope = ? AND attemptKey = ?')
          .setParameter(1, scope).setParameter(2, key).getResultList()
      return rows.isEmpty() ? 0 : ((Number) rows.first()).intValue()
    } as int
  }

  private int storedRowCount(String scope, String key) {
    inTransaction { EntityManager em ->
      def rows = em.createNativeQuery(
          'SELECT COUNT(*) FROM auth_attempt WHERE scope = ? AND attemptKey = ?')
          .setParameter(1, scope).setParameter(2, key).getResultList()
      return ((Number) rows.first()).intValue()
    } as int
  }
}
