package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.datamanager.AppConfig
import life.qbic.identity.application.verification.PasswordResetTokenInvalidator
import life.qbic.identity.domain.model.verification.TokenPurpose
import life.qbic.identity.domain.model.verification.VerificationToken
import life.qbic.identity.domain.repository.UserRepository
import life.qbic.identity.domain.repository.VerificationTokenRepository
import life.qbic.identity.infrastructure.verification.JpaVerificationTokenRepository
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>A completed password change must invalidate outstanding reset links — including on a real one.</b>
 *
 * <p>This spec exists because of a second real 500 on the running server, after the throttle one:</p>
 *
 * <pre>
 * HTTP 500 · No active transaction for update or delete query
 *   at JpaVerificationTokenRepository.supersedeAllFor
 *   at AppConfig.lambda$userRegistrationService$0
 *   at IdentityService.newUserPassword
 * </pre>
 *
 * <p>The invalidation is a JPQL bulk {@code UPDATE} on a container-managed {@code EntityManager}, and
 * the wiring in {@code AppConfig} was a lambda over the repository: it runs after the password update
 * has already committed, so no transaction was active. The password had in fact been changed when the
 * visitor saw the error page, which is the worst shape a failure can take.</p>
 *
 * <h3>Why the existing spec missed it</h3>
 * <p>{@code PasswordChangeInvalidatesResetsSpec} passes a {@code Consumer} that records the user id in
 * a list. That proves the collaborator is <em>called</em>, not that the call can run: it never touches
 * an {@code EntityManager}, so it cannot observe a missing transaction. Same gap, same lesson as
 * {@code AuthAttemptThrottleTransactionSpec}.</p>
 *
 * @since 1.20.2
 */
class PasswordResetInvalidationTransactionSpec extends Specification {

  @Shared
  EntityManagerFactory emf

  static final Instant NOW = Instant.parse('2026-11-13T12:00:00Z')
  static final String USER_ID = 'user-1'
  static final String OUTSTANDING_HASH = 'a' * 64
  static final String CONSUMED_HASH = 'b' * 64

  static String jdbcUrl() {
    System.getProperty('verification.jdbc.url',
        'jdbc:h2:mem:resetinvalidation;MODE=MySQL;DB_CLOSE_DELAY=-1')
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
    emf = new MetadataSources(builder.build())
        .addAnnotatedClass(VerificationToken)
        .buildMetadata()
        .buildSessionFactory()
    createSchema()
  }

  def cleanupSpec() {
    emf?.close()
  }

  def setup() {
    inTransaction { EntityManager em ->
      em.createNativeQuery('DELETE FROM user_verification_token').executeUpdate()
    }
    inTransaction { EntityManager em ->
      // one link the owner never used, one that was already consumed
      em.persist(VerificationToken.issue(USER_ID, TokenPurpose.PASSWORD_RESET, OUTSTANDING_HASH,
          NOW, Duration.ofMinutes(15)))
      def used = VerificationToken.issue(USER_ID, TokenPurpose.PASSWORD_RESET, CONSUMED_HASH,
          NOW, Duration.ofMinutes(15))
      used.consume(NOW.plusSeconds(1))
      em.persist(used)
    }
  }

  // ---- the contract that was missing ---------------------------------------------------------

  def "the invalidation declares @Transactional"() {
    // The annotation is the fix. Without it the bulk UPDATE below throws under a real request.
    given:
    def method = PasswordResetTokenInvalidator.getMethod('invalidateOutstanding', String)

    expect:
    method.isAnnotationPresent(Transactional)
    !method.getAnnotation(Transactional).readOnly()
  }

  def "AppConfig wires the transactional bean, not a lambda over the repository"() {
    // This is what actually broke: the annotation on a class nobody called would have fixed nothing.
    // Reverting the wiring to a lambda is the likely mistake, and it must fail here.
    given:
    def factoryMethod = AppConfig.declaredMethods.find { it.name == 'userRegistrationService' }

    expect: 'the bean method exists'
    factoryMethod != null

    and: 'it takes the transactional collaborator'
    factoryMethod.parameterTypes.contains(PasswordResetTokenInvalidator)

    and: 'it no longer takes the repository, which is what made the lambda possible'
    !factoryMethod.parameterTypes.contains(VerificationTokenRepository)
  }

  // ---- reproduction and end-to-end shape ------------------------------------------------------

  def "the raw invalidation throws when called without a transaction, which is why the bean is required"() {
    // This is the reported 500, reproduced in isolation: the exact call the previous wiring made.
    given:
    def invalidator = new PasswordResetTokenInvalidator(
        new JpaVerificationTokenRepository(emf.createEntityManager()))

    when: 'called with no transaction, as the lambda was'
    invalidator.invalidateOutstanding(USER_ID)

    then:
    thrown(Exception)
  }

  def "through a transaction the outstanding link is superseded and the consumed one is left alone"() {
    when: 'the call runs inside a transaction, as the @Transactional proxy provides in production'
    def superseded = invalidateWithOwnTransaction()

    then: 'exactly the still-usable token was invalidated'
    superseded == 1

    and: 'the outstanding link can no longer be used'
    tokenRow(OUTSTANDING_HASH).supersededAt != null

    and: 'the consumed token keeps its original end-state, not a supersededAt stamp'
    tokenRow(CONSUMED_HASH).supersededAt == null
    tokenRow(CONSUMED_HASH).consumedAt != null
  }

  def "invalidating an account with no outstanding link is a no-op"() {
    expect:
    invalidateWithOwnTransaction('someone-else') == 0
  }

  // ---- helpers -------------------------------------------------------------------------------

  /**
   * Opens a transaction around the call, mirroring the {@code @Transactional} proxy that production
   * gets from Spring.
   */
  private int invalidateWithOwnTransaction(String userId = USER_ID) {
    EntityManager em = emf.createEntityManager()
    try {
      em.transaction.begin()
      def superseded = new PasswordResetTokenInvalidator(new JpaVerificationTokenRepository(em))
          .invalidateOutstanding(userId)
      em.transaction.commit()
      return superseded
    } catch (Exception e) {
      if (em.transaction.active) {
        em.transaction.rollback()
      }
      throw e
    } finally {
      em.close()
    }
  }

  private Map tokenRow(String tokenHash) {
    inTransaction { EntityManager em ->
      def rows = em.createNativeQuery(
          'SELECT consumedAt, supersededAt FROM user_verification_token WHERE tokenHash = ?')
          .setParameter(1, tokenHash)
          .getResultList()
      assert rows.size() == 1
      def row = (Object[]) rows.first()
      return [consumedAt: row[0], supersededAt: row[1]]
    } as Map
  }

  private void createSchema() {
    inTransaction { EntityManager em ->
      em.createNativeQuery('''CREATE TABLE IF NOT EXISTS user_verification_token (
          id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
          userId varchar(255) NOT NULL,
          purpose varchar(32) NOT NULL,
          tokenHash char(64) NOT NULL,
          createdAt datetime(6) NOT NULL,
          expiresAt datetime(6) NOT NULL,
          consumedAt datetime(6) DEFAULT NULL,
          supersededAt datetime(6) DEFAULT NULL,
          UNIQUE KEY uk_user_verification_token_hash (tokenHash)
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
}
