package life.qbic.datamanager.configuration

import spock.lang.Specification

import java.sql.SQLException

/**
 * <b>The startup check reports a missing migration, and nothing else.</b>
 *
 * <p>The failure it exists for is real and was observed: the reminder job logged "Unknown column
 * 'mg1_0.reminderDueSentAt'" and an exception class name, which is accurate and useless — the actual
 * consequence was that <em>every password login</em> failed, because the login guard loads that entity
 * on every login. These assertions cover both halves: recognise the schema failure, and do not diagnose
 * a connection problem as a missing migration.</p>
 */
class MfaSchemaPreflightSpec extends Specification {

    def "a driver-level column error is recognised through its SQL state"() {
        given: "the shape the MariaDB driver produces: wrapped twice, state preserved"
        def driver = new SQLException("Unknown column 'mg1_0.reminderDueSentAt' in 'SELECT'", "42S22")
        def wrapped = new RuntimeException("could not extract ResultSet",
                new RuntimeException("nested", driver))

        expect:
        MfaSchemaPreflight.hintFor(wrapped).isPresent()
    }

    def "the message alone is enough when the state was lost"() {
        given: "some drivers drop the state when they rethrow"
        def failure = new RuntimeException("Unknown column 'mg1_0.reminderDueSentAt' in 'SELECT'")

        expect:
        MfaSchemaPreflight.hintFor(failure).isPresent()
    }

    def "the hint names the SQL to run rather than the symptom"() {
        when:
        def hint = MfaSchemaPreflight.hintFor(new RuntimeException("Unknown column 'x'")).get()

        then: "a log line that only says 'InvalidDataAccessResourceUsageException' costs an hour"
        hint.contains("docs/migrations/NEXT.md")
        hint.contains("reminderDueSentAt")
        hint.contains("every password login fails")
    }

    def "a connection failure is not reported as a missing migration"() {
        given: "the check must not send an operator to a migration that is already applied"
        def failure = new SQLException("Connection refused", "08001")

        expect:
        MfaSchemaPreflight.hintFor(failure).isEmpty()
    }

    def "an unrelated exception is not reported as a missing migration"() {
        expect:
        MfaSchemaPreflight.hintFor(new IllegalStateException("something else")).isEmpty()
    }

    def "a self-referencing cause chain terminates instead of hanging the startup check"() {
        given: "a Throwable whose cause is itself is contrived but not impossible in a wrapper"
        def looping = new RuntimeException("loop") {
            @Override
            Throwable getCause() {
                return this
            }
        }

        expect:
        MfaSchemaPreflight.hintFor(looping).isEmpty()
    }

    def "the probe is a read that returns no rows, so it costs nothing on a healthy database"() {
        expect:
        MfaSchemaPreflight.COLUMN_PROBE.contains("WHERE 1 = 0")
        MfaSchemaPreflight.COLUMN_PROBE.contains("reminderDueSentAt")
        MfaSchemaPreflight.COLUMN_PROBE.contains("reminderFinalSentAt")
    }
}
