package life.qbic.identity.infrastructure.mfa

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import life.qbic.identity.domain.model.mfa.MfaGrace
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>A grace write must never deny a login, and must never move a deadline that already exists.</b>
 *
 * <p>Observed on a deployed instance: a row was committed (readable from a SQL client with the very
 * timestamp the login used), the login's read-back returned nothing, the adapter threw, and every
 * attempt of that account got an HTTP 500.</p>
 *
 * <p>Root cause: the read-back asked the wrong question. At MariaDB's default isolation
 * (<b>REPEATABLE READ</b>) a plain {@code SELECT} keeps reading the snapshot taken at the transaction's
 * first read, so a row committed by a <em>concurrent</em> login stays invisible to this transaction — the
 * {@code ON DUPLICATE KEY} insert reports "existing row", and the {@code SELECT} afterwards reports
 * nothing.</p>
 *
 * <p>So the insert's own row count is now the authority: <b>1 means this call inserted</b>, and no read
 * is needed at all - which removes the failing read from the common path entirely. Only "someone else's
 * row exists" needs a read, and that one is a locking read, which InnoDB evaluates against the current
 * state in every isolation level.</p>
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
    Query probeQuery = Mock()

    JpaMfaGraceRepository repository = new JpaMfaGraceRepository(entityManager)

    def setup() {
        entityManager.createNativeQuery(_ as String) >> { String sql ->
            sql.contains("INSERT INTO user_mfa_grace") ? insertQuery : probeQuery
        }
        insertQuery.setParameter(_, _) >> insertQuery
        probeQuery.getSingleResult() >> "data_management"
        entityManager.createQuery(_, MfaGrace) >> ormRead
        ormRead.setParameter(_, _) >> ormRead
        ormRead.setLockMode(_) >> ormRead
    }

    def "the candidate is returned without any read when this call inserted the row"() {
        given: "affected rows 1 means the INSERT inserted, and that is the whole question"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        insertQuery.executeUpdate() >> 1

        when:
        def result = repository.startIfAbsent(candidate)

        then: "no SELECT runs, so no snapshot can hide the row this call just wrote"
        result == candidate
        0 * entityManager.createQuery(_, MfaGrace)
    }

    def "an existing row's deadline wins, and cannot be moved by a later login"() {
        given: "affected rows 0: ON DUPLICATE KEY found somebody else's row"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        def stored = MfaGrace.start("u-1", NOW.minus(Duration.ofDays(1)), WINDOW)
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> [stored]

        expect: "grace is not renewable by logging in"
        repository.startIfAbsent(candidate) == stored
    }

    def "that read is a locking read, because it reads somebody else's committed row"() {
        given:
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> [MfaGrace.start("u-1", NOW, WINDOW)]

        when:
        repository.startIfAbsent(MfaGrace.start("u-1", NOW, WINDOW))

        then: "a current read in every isolation level, unlike a plain SELECT"
        1 * ormRead.setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) >> ormRead
    }

    def "an unreadable existing row returns the candidate instead of failing the login"() {
        given: "the observed failure shape: the insert says a row exists, the read cannot see it"
        def candidate = MfaGrace.start("u-1", NOW, WINDOW)
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> []

        expect: "no exception — no caller reads this value, and the session is legitimate"
        repository.startIfAbsent(candidate) == candidate
    }

    def "the diagnostic names the connection, not just the symptom"() {
        given:
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> []
        probeQuery.getSingleResult() >> "value"

        when:
        repository.startIfAbsent(MfaGrace.start("u-1", NOW, WINDOW))

        then: "schema, autocommit, isolation and connection id are all probed"
        (1.._) * probeQuery.getSingleResult() >> "value"
        noExceptionThrown()
    }

    def "a broken probe does not turn the fallback into a failure"() {
        given:
        insertQuery.executeUpdate() >> 0
        ormRead.getResultList() >> []
        probeQuery.getSingleResult() >> { throw new IllegalStateException("no database selected") }

        expect:
        repository.startIfAbsent(MfaGrace.start("u-1", NOW, WINDOW)) != null
    }
}
