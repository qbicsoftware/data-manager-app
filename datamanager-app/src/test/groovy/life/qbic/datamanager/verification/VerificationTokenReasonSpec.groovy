package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.api.VerificationLinkService.Purpose
import life.qbic.identity.domain.model.verification.ConsumeFailure
import life.qbic.identity.domain.model.verification.TokenPurpose
import life.qbic.identity.domain.model.verification.VerificationToken
import life.qbic.identity.domain.model.verification.VerificationTokens
import life.qbic.identity.domain.repository.VerificationTokenRepository
import life.qbic.identity.infrastructure.verification.JpaVerificationTokenRepository
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Rejection reasons, asserted at the layer that legitimately exposes them.</b>
 *
 * <p>The service API deliberately reports only "refused" — AUTH-NFR-03 forbids telling an
 * unauthenticated caller <em>why</em>, because "expired" vs "unknown" discloses account state. That
 * makes the reasons untestable through the service, so they are asserted here against the repository
 * contract, which returns {@link ConsumeFailure} for logging and diagnostics.</p>
 *
 * <p>This is the pairing that keeps both properties honest: the reasons are correct <em>and</em> they
 * never escape to the user.</p>
 */
class VerificationTokenReasonSpec extends Specification {

    @Shared
    EntityManagerFactory emf

    def setupSpec() {
        def builder = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.JAKARTA_JDBC_URL,
                        "jdbc:h2:mem:vertokenreason;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
                .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
                .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
        def registry = builder.build()
        emf = new MetadataSources(registry)
                .addAnnotatedClass(VerificationToken)
                .buildMetadata()
                .buildSessionFactory()
    }

    def cleanupSpec() {
        emf?.close()
    }

    def setup() {
        truncate()
    }

    def "an unknown hash is reported as UNKNOWN"() {
        when:
        def result = consume(VerificationTokens.hash("never-issued"), TokenPurpose.EMAIL_CONFIRMATION,
                Instant.now())

        then:
        result instanceof VerificationTokenRepository.Refused
        (result as VerificationTokenRepository.Refused).failure() == ConsumeFailure.UNKNOWN
    }

    def "an expired token is reported as EXPIRED, not UNKNOWN"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = persist(TokenPurpose.PASSWORD_RESET, now, Duration.ofMinutes(5))

        when:
        def result = consume(issued, TokenPurpose.PASSWORD_RESET, now.plusSeconds(301))

        then: "the distinction exists for logs, and must be accurate there"
        (result as VerificationTokenRepository.Refused).failure() == ConsumeFailure.EXPIRED
    }

    def "a consumed token is reported as ALREADY_CONSUMED"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def hash = persist(TokenPurpose.EMAIL_CONFIRMATION, now, Duration.ofHours(1))
        consume(hash, TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(1))

        when:
        def result = consume(hash, TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(2))

        then:
        (result as VerificationTokenRepository.Refused).failure() == ConsumeFailure.ALREADY_CONSUMED
    }

    def "a superseded token is reported as SUPERSEDED"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def hash = persist(TokenPurpose.EMAIL_CONFIRMATION, now, Duration.ofHours(1))
        supersedeAll(TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(5))

        when:
        def result = consume(hash, TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(10))

        then:
        (result as VerificationTokenRepository.Refused).failure() == ConsumeFailure.SUPERSEDED
    }

    def "a token presented for the wrong flow is reported as WRONG_PURPOSE"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def hash = persist(TokenPurpose.EMAIL_CONFIRMATION, now, Duration.ofHours(1))

        when: "a confirmation token presented at the reset endpoint"
        def result = consume(hash, TokenPurpose.PASSWORD_RESET, now.plusSeconds(1))

        then:
        (result as VerificationTokenRepository.Refused).failure() == ConsumeFailure.WRONG_PURPOSE
    }

    // ------------------------------------------------------------------ helpers

    String persist(TokenPurpose purpose, Instant issuedAt, Duration lifetime) {
        String raw = VerificationTokens.newRawToken()
        String hash = VerificationTokens.hash(raw)
        def token = VerificationToken.issue("u-1", purpose, hash, issuedAt, lifetime)
        tx { JpaVerificationTokenRepository repo -> repo.save(token) }
        return hash
    }

    Object consume(String hash, TokenPurpose purpose, Instant now) {
        tx { JpaVerificationTokenRepository repo -> repo.consumeIfUsable(hash, purpose, now) }
    }

    void supersedeAll(TokenPurpose purpose, Instant now) {
        tx { JpaVerificationTokenRepository repo ->
            repo.supersedeAllFor("u-1", purpose, "f" * 64, now)
        }
    }

    void truncate() {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            em.createNativeQuery("DELETE FROM user_verification_token").executeUpdate()
            em.transaction.commit()
        } finally {
            em.close()
        }
    }

    private def tx(Closure c) {
        EntityManager em = emf.createEntityManager()
        def tx = em.transaction
        tx.begin()
        try {
            def result = c(new JpaVerificationTokenRepository(em))
            tx.commit()
            return result
        } catch (Throwable t) {
            if (tx.active) tx.transaction?.rollback()
            throw t
        } finally {
            em.close()
        }
    }
}
