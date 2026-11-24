package life.qbic.identity.infrastructure.mfa

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import life.qbic.identity.domain.model.mfa.MfaGrace
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>A grace write must never deny a login.</b>
 *
 * <p>Observed on a deployed instance: the insert succeeded, the read-back that follows it returned
 * nothing, and the adapter threw — so <em>every</em> login attempt of that account got a 500. The read is
 * an assertion whose result no caller reads; failing the request over it is the wrong trade in an
 * authentication path.</p>
 *
 * <p>These tests pin what happens when the read-back misbehaves, which is exactly the state that could
 * not be reproduced anywhere else.</p>
 *
 * <p>Lives in this module because it is where the Groovy/Spock test setup is; the class under test is
 * plain Java in {@code identity-infrastructure}, which has no test sources of its own.</p>
 */
class JpaMfaGraceRepositorySpec extends Specification {

    static final Instant NOW = Instant.parse("2026-11-24T09:00:00Z")
    static final Duration WINDOW = Duration.ofHours(48)

    EntityManager entityManager = Mock()
    Query insertQuery = Mock()
    TypedQuery<MfaGrace> ormRead = Mock()
    Query nativeRead = Mock()
    Query schemaQuery = Mock()

    JpaMfaGraceRepository repository = new JpaMfaGraceRepository(entityManager)

    /** The insert is the first native query the adapter runs; nothing else is stubbed yet. */
    private void insertAffects(int rows) {
        entityManager.createNativeQuery(_) >> insertQuery
        insertQuery.setParameter(_, _) >> insertQuery
        insertQuery.executeUpdate() >> rows
    }

    /** Insert, then the native re-read, then the schema probe: the adapter's order, in order. */
    private void thenNativeReadAndSchema() {
        entityManager.createNativeQuery(_) >>> [insertQuery, nativeRead, schemaQuery]
        insertQuery.setParameter(_, _) >> insertQuery
        insertQuery.executeUpdate() >> 1
        nativeRead.setParameter(_, _) >> nativeRead
        nativeRead.getResultList() >> []
        schemaQuery.getSingleResult() >> "data_management"
    }

    def "the stored row is returned when the read-back works"() {
        given: "the normal case: insert, then the row is readable"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        def stored = MfaGrace.start("u-1", NOW.minus(Duration.ofHours(10)), WINDOW)
        insertAffects(0)
        entityManager.createQuery(_, MfaGrace) >> ormRead
        ormRead.setParameter(_, _) >> ormRead
        ormRead.getResultList() >> [stored]

        expect:
        repository.startIfAbsent(candidate) == stored
    }

    def "an unreadable row returns the candidate instead of failing the login"() {
        given: "the observed failure: written, then invisible to both reads"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        thenNativeReadAndSchema()
        entityManager.createQuery(_, MfaGrace) >> ormRead
        ormRead.setParameter(_, _) >> ormRead
        ormRead.getResultList() >> []

        expect: "no exception — the write happened, the session is legitimate"
        repository.startIfAbsent(candidate) == candidate
    }

    def "a row the ORM query misses is still returned when a native read can see it"() {
        given: "the mapping and the table disagree, which is a different bug from a missing row"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        entityManager.createNativeQuery(_) >>> [insertQuery, nativeRead]
        insertQuery.setParameter(_, _) >> insertQuery
        insertQuery.executeUpdate() >> 0
        entityManager.createQuery(_, MfaGrace) >> ormRead
        ormRead.setParameter(_, _) >> ormRead
        ormRead.getResultList() >> []
        nativeRead.setParameter(_, _) >> nativeRead
        nativeRead.getResultList() >> [
                ["u-1", java.sql.Timestamp.from(NOW), java.sql.Timestamp.from(NOW.plus(WINDOW))] as Object[]
        ]

        when:
        def result = repository.startIfAbsent(candidate)

        then: "the stored deadline wins, so a second login cannot move it"
        result.graceUntil() == NOW.plus(WINDOW)
        result.startedAt() == NOW
    }

    def "the schema is named when the failure cannot be resolved"() {
        given: "the log line must answer 'which table did this connection write to?'"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        thenNativeReadAndSchema()
        entityManager.createQuery(_, MfaGrace) >> ormRead
        ormRead.setParameter(_, _) >> ormRead
        ormRead.getResultList() >> []

        when:
        repository.startIfAbsent(candidate)

        then: "one lookup of the effective schema, no exception"
        1 * schemaQuery.getSingleResult() >> "data_management"
        noExceptionThrown()
    }

    def "a broken schema probe does not turn the fallback into a failure"() {
        given:
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        thenNativeReadAndSchema()
        entityManager.createQuery(_, MfaGrace) >> ormRead
        ormRead.setParameter(_, _) >> ormRead
        ormRead.getResultList() >> []
        schemaQuery.getSingleResult() >> { throw new IllegalStateException("no database selected") }

        expect:
        repository.startIfAbsent(candidate) == candidate
    }
}
