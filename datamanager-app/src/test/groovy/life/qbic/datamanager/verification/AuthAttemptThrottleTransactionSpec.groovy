package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.infrastructure.throttle.JpaAuthAttemptCounter
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Method
import java.time.Duration
import java.time.Instant

/**
 * <b>The throttle must work when the caller does NOT open a transaction — because production does not.</b>
 *
 * <p>This spec exists because of a real 500. On a running server, every failed login threw:</p>
 *
 * <pre>
 * HTTP 500 · org.hibernate.TransactionRequiredException: Executing an update/delete query
 *   at JpaAuthAttemptCounter.increment
 *   at AuthAttemptThrottle.record
 *   at AuthAttemptEventListener.onFailure
 *   at SimpleApplicationEventMulticaster.multicastEvent
 * </pre>
 *
 * <p>The counter uses native SQL on a container-managed {@code EntityManager}. With no active
 * transaction an {@code INSERT} throws — and because the login counter is written from a Spring
 * authentication <em>event listener</em>, the exception escaped through the event publisher as an
 * HTTP 500, <em>after</em> the credentials had already been rejected. So a wrong password produced a
 * server error instead of "Incorrect username or password".</p>
 *
 * <h3>Why the existing specs missed it</h3>
 * <p>Every case in {@code AuthAttemptCounterConcurrencySpec} wraps
 * {@code new JpaAuthAttemptCounter(em).increment(...)} in its own {@code inTransaction { }} helper.
 * They supplied the transaction boundary that production never opened. A test that hands the code
 * something it does not have in production proves nothing about production.</p>
 *
 * <p>This spec therefore asserts the <em>declared</em> transaction contract of the service, and
 * reproduces the failure directly: the same call with no transaction must throw, which is what makes
 * the annotation load-bearing rather than decorative.</p>
 *
 * @since 1.0.0
 */
class AuthAttemptThrottleTransactionSpec extends Specification {

  @Shared
  EntityManagerFactory emf

  static final Instant T0 = Instant.parse('2026-11-11T10:00:00Z')
  static final String ADDRESS = 'alice@example.org'

  static String jdbcUrl() {
    System.getProperty('verification.jdbc.url',
        'jdbc:h2:mem:auththrottletx;MODE=MySQL;DB_CLOSE_DELAY=-1')
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

  def setupSpec() {
    def builder = new StandardServiceRegistryBuilder()
        .applySetting(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl())
        .applySetting(AvailableSettings.JAKARTA_JDBC_USER, jdbcUser())
        .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, jdbcPassword())
        .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, jdbcDriver())
    def dialect = System.getProperty('verification.dialect')
    if (dialect != null) {
      builder.applySetting(AvailableSettings.DIALECT, dialect)
    }
    emf = new MetadataSources(builder.build()).buildMetadata().buildSessionFactory()
    createSchema()
  }

  def cleanupSpec() {
    emf?.close()
  }

  // ---- the contract that was missing ---------------------------------------------------------

  def "every public write method on the throttle declares @Transactional"() {
    // The annotation is the fix. Without it the native SQL below throws under a real request, and the
    // failure appears as a 500 on a page that should have shown "Incorrect username or password".
    expect:
    transactionallyAnnotated('record')
    transactionallyAnnotated('onSuccess')
    transactionallyAnnotated('isThrottled')
  }

  def "the read-only path is marked readOnly so it does not take a write lock per login"() {
    given:
    Method isThrottled = AuthAttemptThrottle.getMethod('isThrottled',
        AuthAttemptThrottle.Scope, String, Instant)

    when:
    def annotation = isThrottled.getAnnotation(
        org.springframework.transaction.annotation.Transactional)

    then: 'present, and read-only — this runs on every login attempt'
    annotation != null
    annotation.readOnly()
  }

  def "the writes are NOT readOnly, or the upsert would still fail"() {
    // A copy-paste of readOnly = true onto record() would leave the 500 in place while looking fixed.
    expect:
    !readOnly('record')
    !readOnly('onSuccess')
  }

  // ---- reproduction: the same call without a transaction must fail ---------------------------

  def "the counter throws when called without a transaction, which is why the annotation is required"() {
    // This is the bug, reproduced in isolation. If Hibernate ever stopped requiring a transaction for
    // native DML, this spec would fail and the annotation above would become redundant rather than
    // load-bearing — which is information, not noise.
    given:
    def counter = new JpaAuthAttemptCounter(emf.createEntityManager())
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)

    when: 'the exact call the event listener makes, with no transaction'
    counter.increment('LOGIN', key, T0, Duration.ofMinutes(15))

    then: 'it fails — the caller must provide the boundary'
    thrown(Exception)
  }

  def "through the real throttle with a transaction, a failed login counts instead of throwing"() {
    // The end-to-end shape of the fix: a transaction-scoped proxy around the same object. This is what
    // Spring's @Transactional does at runtime, made explicit so it can be asserted without a context.
    given: 'a counter whose every call runs in its own transaction, as @Transactional provides'
    def throttle = throttleWithOwnTransactions(3)
    def key = AuthAttemptThrottle.attemptKey(AuthAttemptThrottle.Scope.LOGIN, ADDRESS)

    when: 'three attempts from the event listener'
    3.times {
      assert throttle.record(AuthAttemptThrottle.Scope.LOGIN, ADDRESS, T0)
          == AuthAttemptThrottle.Decision.ALLOWED
    }

    then: 'the counter advanced and did not throw'
    storedCount('LOGIN', key) == 3
  }

  def "a login that is never recorded cannot reach the block threshold"() {
    // Expresses the user-visible consequence of the bug: with the 500, the count never advanced at
    // all, so an attacker could guess passwords forever — the throttle was not merely noisy, it was
    // absent.
    given:
    def throttle = throttleWithOwnTransactions(3)

    when: 'an attacker fails four times with a transaction present'
    4.times { throttle.record(AuthAttemptThrottle.Scope.LOGIN, ADDRESS, T0) }

    then: 'the block engaged'
    throttle.isThrottled(AuthAttemptThrottle.Scope.LOGIN, ADDRESS, T0.plusSeconds(1))
  }

  // ---- helpers -------------------------------------------------------------------------------

  /**
   * A throttle whose counter opens a transaction per call — the explicit stand-in for what Spring's
   * {@code @Transactional} proxy does at runtime when the real application serves a request.
   */
  private AuthAttemptThrottle throttleWithOwnTransactions(int maxAttempts) {
    return new AuthAttemptThrottle(new PerCallCounter(this),
        maxAttempts, Duration.ofMinutes(15), Duration.ofMinutes(15),
        2, Duration.ofMinutes(15), Duration.ofMinutes(15))
  }

  private static boolean transactionallyAnnotated(String methodName) {
    return AuthAttemptThrottle.declaredMethods
        .findAll { it.name == methodName }
        .any { it.isAnnotationPresent(org.springframework.transaction.annotation.Transactional) }
  }

  private static boolean readOnly(String methodName) {
    return AuthAttemptThrottle.declaredMethods
        .findAll { it.name == methodName }
        .any {
          it.getAnnotation(org.springframework.transaction.annotation.Transactional)
              ?.readOnly() == true
        }
  }

  private void createSchema() {
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

  /** Opens a transaction around every counter call, mirroring the @Transactional proxy. */
  static final class PerCallCounter implements life.qbic.identity.domain.repository.AuthAttemptCounter {

    private final AuthAttemptThrottleTransactionSpec spec

    PerCallCounter(AuthAttemptThrottleTransactionSpec spec) {
      this.spec = spec
    }

    @Override
    life.qbic.identity.domain.repository.AuthAttemptCounter.Counter increment(
        String scope, String key, Instant now, Duration window) {
      spec.inTransaction { EntityManager em ->
        new JpaAuthAttemptCounter(em).increment(scope, key, now, window)
      }
    }

    @Override
    life.qbic.identity.domain.repository.AuthAttemptCounter.Counter peek(String scope, String key) {
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
}
