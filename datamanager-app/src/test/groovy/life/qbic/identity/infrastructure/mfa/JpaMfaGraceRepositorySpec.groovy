package life.qbic.identity.infrastructure.mfa

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import life.qbic.identity.domain.model.mfa.MfaGrace
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>A grace write must never deny a login, and must never lie about what it stored.</b>
 *
 * <p>Observed on a deployed instance: the row was committed — it could be read from a SQL client
 * afterwards, with the very timestamp the login had used — yet the read-back inside the same method
 * returned nothing, and the adapter threw. Every login attempt of that account got a 500.</p>
 *
 * <p>Two lessons are pinned here. The read is an <b>assertion no caller reads</b>, so it must not be
 * fatal. And after a write the question is "what does the row say <em>now</em>", which is a
 * <b>current read</b> ({@code FOR UPDATE}), not the snapshot a plain {@code SELECT} answers from — the
 * distinction that produced this failure.</p>
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
    Query probeQuery = Mock()

    JpaMfaGraceRepository repository = new JpaMfaGraceRepository(entityManager)

    def setup() {
        // Answer by statement text: the adapter issues three kinds of native query and the diagnostics
        // probe add more, so matching on order would break whenever a probe is added.
        entityManager.createNativeQuery(_ as String) >> { String sql -> queryFor(sql) }
        insertQuery.setParameter(_, _) >> insertQuery
        nativeRead.setParameter(_, _) >> nativeRead
        probeQuery.getSingleResult() >> "data_management"
        entityManager.createQuery(_, MfaGrace) >> ormRead
        ormRead.setParameter(_, _) >> ormRead
        ormRead.setLockMode(_) >> ormRead
    }

    private Query queryFor(String sql) {
        if (sql.contains("INSERT INTO user_mfa_grace")) {
            return insertQuery
        }
        if (sql.contains("SELECT userId")) {
            return nativeRead
        }
        return probeQuery
    }

    def "the stored row is returned when the read-back works"() {
        given: "the normal case: insert inserted, then the row is readable"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        def stored = MfaGrace.start("u-1", NOW.minus(Duration.ofHours(10)), WINDOW)
        insertQuery.executeUpdate() >> 1
        ormRead.getResultList() >> [stored]

        expect:
        repository.startIfAbsent(candidate) == stored
    }

    def "the read-back is a current read, not a snapshot read"() {
        given: "after a write the question is what the row says now; a plain SELECT answers from a snapshot"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> [candidate]

        when:
        repository.startIfAbsent(candidate)

        then: "FOR UPDATE sees the latest committed row, which is what produced the observed failure"
        1 * ormRead.setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) >> ormRead
    }

    def "an unreadable row returns the candidate instead of failing the login"() {
        given: "the observed failure: written, then invisible to the ORM and to a native re-read"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> []
        nativeRead.getResultList() >> []

        expect: "no exception — no caller reads this value, and the session is legitimate"
        repository.startIfAbsent(candidate) == candidate
    }

    def "a row the ORM query misses is still returned when a native read can see it"() {
        given: "the mapping and the table disagree, which is a different bug from a missing row"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> []
        nativeRead.getResultList() >> [
                ["u-1", java.sql.Timestamp.from(NOW), java.sql.Timestamp.from(NOW.plus(WINDOW))] as Object[]
        ]

        when:
        def result = repository.startIfAbsent(candidate)

        then: "the stored deadline wins, so a second login cannot move it"
        result.graceUntil() == NOW.plus(WINDOW)
        result.startedAt() == NOW
    }

    def "the diagnostic names the connection, not just the symptom"() {
        given: "the log line must answer 'did the writer and the readers share a connection?'"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> []
        nativeRead.getResultList() >> []

        when:
        repository.startIfAbsent(candidate)

        then: "schema, autocommit, isolation and connection id are all probed"
        (1.._) * probeQuery.getSingleResult() >> "value"
        noExceptionThrown()
    }

    def "a broken probe does not turn the fallback into a failure"() {
        given:
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> []
        nativeRead.getResultList() >> []
        probeQuery.getSingleResult() >> { throw new IllegalStateException("no database selected") }

        expect:
        repository.startIfAbsent(candidate) == candidate
    }
}
