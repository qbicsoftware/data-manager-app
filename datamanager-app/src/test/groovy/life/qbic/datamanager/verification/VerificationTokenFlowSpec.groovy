package life.qbic.datamanager.verification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import life.qbic.identity.application.verification.VerificationTokenServiceImpl
import life.qbic.identity.application.verification.VerificationTokenServiceImpl.Consumed
import life.qbic.identity.application.verification.VerificationTokenServiceImpl.Invalid
import life.qbic.identity.domain.model.verification.ConsumeFailure
import life.qbic.identity.domain.model.verification.TokenPurpose
import life.qbic.identity.domain.repository.VerificationTokenRepository
import life.qbic.identity.infrastructure.verification.JpaVerificationTokenRepository
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * <b>SPIKE — verification token flow against real persistence.</b>
 *
 * <p>Answers the questions a DDL review cannot: does the round-trip through Hibernate + a real
 * database preserve the properties the schema was chosen for?</p>
 *
 * <p>Runs on H2 in {@code MODE=MySQL} because {@link life.qbic.identity.infrastructure.verification.JpaAttemptCounter}
 * relies on MySQL/MariaDB {@code ON DUPLICATE KEY UPDATE}; plain H2 rejects that syntax (verified on
 * H2 2.3.232). Without the mode flag these tests would exercise a statement production never runs.</p>
 *
 * <p>In production these specs belong in {@code identity-infrastructure}; they live here in the spike
 * because that module has no test dependencies configured yet.</p>
 */
class VerificationTokenFlowSpec extends Specification {

    @Shared
    EntityManagerFactory emf

    VerificationTokenServiceImpl service

    static final Duration CONFIRMATION = Duration.ofHours(1)
    static final Duration RESET = Duration.ofMinutes(5)
    static final Duration SET_PASSWORD = Duration.ofHours(1)

    def setupSpec() {
        def registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.JAKARTA_JDBC_URL,
                        "jdbc:h2:mem:vertoken;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .applySetting(AvailableSettings.JAKARTA_JDBC_USER, "sa")
                .applySetting(AvailableSettings.JAKARTA_JDBC_PASSWORD, "")
                .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver")
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
                .build()
        def metadata = new MetadataSources(registry)
                .addAnnotatedClass(life.qbic.identity.domain.model.verification.VerificationToken)
                .buildMetadata()
        emf = metadata.buildSessionFactory()
    }

    def cleanupSpec() {
        emf?.close()
    }

    def setup() {
        truncate()
        service = newService(CONFIRMATION, RESET, SET_PASSWORD)
    }

    /**
     * Builds the service with one EntityManager per repository call.
     *
     * <p>This mirrors a request-scoped EntityManager: each call is its own unit of work. Real
     * production wiring would need consumption and the state change it authorises (confirming an
     * address, setting a password) in ONE transaction — see the spike notes.</p>
     */
    VerificationTokenServiceImpl newService(Duration confirmation, Duration reset, Duration setPw) {
        new VerificationTokenServiceImpl(new PerCallRepository(emf), confirmation, reset, setPw)
    }

    def truncate() {
        EntityManager em = emf.createEntityManager()
        try {
            em.transaction.begin()
            em.createNativeQuery("DELETE FROM user_verification_token").executeUpdate()
            em.transaction.commit()
        } finally {
            em.close()
        }
    }

    // ---------------------------------------------------------------- happy path

    def "a confirmation token is usable exactly once"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = service.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, now)

        when: "first use"
        def first = service.consume(issued.rawToken(), TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(60))

        then:
        first instanceof Consumed
        (first as Consumed).userId() == "u-1"

        when: "second use of the same link"
        def second = service.consume(issued.rawToken(), TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(61))

        then: "the link is dead — this is AUTH-R-05's single-use clause"
        second instanceof Invalid
        (second as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.ALREADY_CONSUMED
    }

    def "the raw token is never stored, only its SHA-256 hash"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")

        when:
        def issued = service.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, now)

        then: "the raw token does not appear anywhere in the table"
        !persistedColumnValues().contains(issued.rawToken())

        and: "the stored hash is the SHA-256 of the raw token"
        persistedTokenHashes() == [life.qbic.identity.domain.model.verification.VerificationTokens.hash(issued.rawToken())]
    }

    def "a token is rejected after its configured lifetime"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = service.issue("u-1", TokenPurpose.PASSWORD_RESET, now)

        expect: "the reset lifetime is 5 minutes here"
        issued.expiresAt() == now.plus(RESET)

        when: "consumed one second past expiry"
        def outcome = service.consume(issued.rawToken(), TokenPurpose.PASSWORD_RESET, now.plus(RESET).plusSeconds(1))

        then:
        outcome instanceof Invalid
        (outcome as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.EXPIRED
    }

    def "a token is still usable one second before expiry"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = service.issue("u-1", TokenPurpose.PASSWORD_RESET, now)

        when:
        def outcome = service.consume(issued.rawToken(), TokenPurpose.PASSWORD_RESET,
                now.plus(RESET).minusSeconds(1))

        then:
        outcome instanceof Consumed
    }

    // ---------------------------------------------------------------- supersede

    def "issuing a new token for the same purpose invalidates the previous one"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def first = service.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, now)
        def second = service.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(30))

        when: "the older link is followed"
        def oldOutcome = service.consume(first.rawToken(), TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(60))

        then:
        oldOutcome instanceof Invalid
        (oldOutcome as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.SUPERSEDED

        when: "the newer link is followed"
        def newOutcome = service.consume(second.rawToken(), TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(61))

        then:
        newOutcome instanceof Consumed
    }

    def "superseding does not touch a different purpose for the same user"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def confirmation = service.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, now)
        def reset = service.issue("u-1", TokenPurpose.PASSWORD_RESET, now)

        when: "a second reset is issued, superseding the first reset"
        service.issue("u-1", TokenPurpose.PASSWORD_RESET, now.plusSeconds(10))

        then: "the confirmation token is untouched"
        def outcome = service.consume(confirmation.rawToken(), TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(60))
        outcome instanceof Consumed

        and: "the first reset token was superseded"
        def resetOutcome = service.consume(reset.rawToken(), TokenPurpose.PASSWORD_RESET, now.plusSeconds(60))
        (resetOutcome as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.SUPERSEDED
    }

    // ---------------------------------------------------------------- cross-purpose

    def "a confirmation token cannot be used to reset a password"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def confirmation = service.issue("u-1", TokenPurpose.EMAIL_CONFIRMATION, now)

        when: "presented at the reset endpoint"
        def outcome = service.consume(confirmation.rawToken(), TokenPurpose.PASSWORD_RESET, now.plusSeconds(10))

        then:
        outcome instanceof Invalid
        (outcome as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.WRONG_PURPOSE

        and: "and it is still usable for its own purpose — a wrong attempt must not burn it"
        service.consume(confirmation.rawToken(), TokenPurpose.EMAIL_CONFIRMATION, now.plusSeconds(11)) instanceof Consumed
    }

    def "an unknown token is reported without touching anything"() {
        given:
        def now = Instant.parse("2026-11-10T10:00:00Z")

        when:
        def outcome = service.consume("not-a-real-token", TokenPurpose.EMAIL_CONFIRMATION, now)

        then:
        outcome instanceof Invalid
        (outcome as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.UNKNOWN
    }

    def "blank and null tokens are rejected as malformed"() {
        expect:
        (service.consume(raw, TokenPurpose.EMAIL_CONFIRMATION, Instant.now()) as Invalid).reason() == life.qbic.identity.domain.model.verification.ConsumeFailure.MALFORMED

        where:
        raw << [null, "", "   "]
    }

    // ---------------------------------------------------------------- time handling

    def "expiry survives a round-trip through the database regardless of JVM default timezone"() {
        given: "the same absolute instant expressed via a non-UTC default zone"
        def originalZone = TimeZone.getDefault()
        def now = Instant.parse("2026-11-10T10:00:00Z")
        def issued = service.issue("u-1", TokenPurpose.PASSWORD_RESET, now)

        when:
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Pacific/Kiritimati"))) // UTC+14
        def outcome = service.consume(issued.rawToken(), TokenPurpose.PASSWORD_RESET, now.plusSeconds(60))

        then: "expiry is evaluated in one clock, so the offset cannot make a valid token look expired"
        outcome instanceof Consumed

        cleanup:
        TimeZone.setDefault(originalZone)
    }

    // ---------------------------------------------------------------- bounds

    def "a configured lifetime beyond the AUTH-R-05 bound fails at construction"() {
        when:
        newService(confirmation, reset, setPassword)

        then:
        thrown(IllegalArgumentException)

        where:
        confirmation         | reset                       | setPassword
        Duration.ofHours(2)  | Duration.ofMinutes(5)       | Duration.ofHours(1)   // confirmation > 1h
        Duration.ofHours(1)  | Duration.ofMinutes(16)      | Duration.ofHours(1)   // reset > 15min
        Duration.ofHours(1)  | Duration.ofMinutes(5)       | Duration.ofHours(2)   // set-password > 1h
    }

    def "a token hash that is not a 64-char lower-case hex digest is rejected"() {
        when:
        life.qbic.identity.domain.model.verification.VerificationToken.issue(
                "u-1", TokenPurpose.EMAIL_CONFIRMATION, hash, Instant.now(), CONFIRMATION)

        then:
        thrown(IllegalArgumentException)

        where:
        hash << [
                "too-short",
                "A" * 64,                       // upper-case is not accepted
                life.qbic.identity.domain.model.verification.VerificationTokens.newRawToken()
        ]
    }

    // ---------------------------------------------------------------- helpers

    List<String> persistedTokenHashes() {
        EntityManager em = emf.createEntityManager()
        try {
            em.createNativeQuery("SELECT tokenHash FROM user_verification_token")
                    .resultList.collect { ((String) it).trim() }
        } finally {
            em.close()
        }
    }

    List<String> persistedColumnValues() {
        EntityManager em = emf.createEntityManager()
        try {
            em.createNativeQuery("SELECT * FROM user_verification_token").resultList
                    .collect { row -> ((Object[]) row).collect { String.valueOf(it) } }
                    .flatten()
                    .collect { it.trim() }
        } finally {
            em.close()
        }
    }

}
