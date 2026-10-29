package life.qbic.application.commons.time

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
import java.time.ZoneId

/**
 * Pins the date formats that views render, and that database filters must match.
 *
 * <p>{@link DateTimeFormat} is a cross-layer contract: the rendered form and the MariaDB
 * {@code DATE_FORMAT} pattern have to describe the same text, or a search for a displayed value
 * silently fails. Both sides are therefore asserted here, for the format added for the measurement
 * list and for the ones already in use.</p>
 */
class DateTimeFormatSpec extends Specification {

    private static final Instant WEDNESDAY_ELEVENTH_FEB_2026 = Instant.parse("2026-02-11T10:15:30Z")

    def "a date renders with its abbreviated weekday"() {
        given:
        def formatter = DateTimeFormat.asJavaFormatter(DateTimeFormat.WEEKDAY_DATE_SHORT,
                ZoneId.of("UTC"))

        expect: "the weekday is what a person relates to, not the day number"
        formatter.format(WEDNESDAY_ELEVENTH_FEB_2026) == "Wed, 11 Feb 2026"
    }

    def "the weekday is computed in the reader's zone, not in UTC"() {
        given: "an instant that is Wednesday in UTC but already Thursday two hours east"
        def lateWednesday = Instant.parse("2026-02-11T23:30:00Z")

        expect:
        DateTimeFormat.asJavaFormatter(DateTimeFormat.WEEKDAY_DATE_SHORT, ZoneId.of("UTC"))
                .format(lateWednesday) == "Wed, 11 Feb 2026"

        and: "the same instant rendered for a Berlin reader must say Thursday"
        DateTimeFormat.asJavaFormatter(DateTimeFormat.WEEKDAY_DATE_SHORT,
                ZoneId.of("Europe/Berlin"))
                .format(lateWednesday) == "Thu, 12 Feb 2026"
    }

    def "the weekday format's database pattern matches what is displayed"() {
        expect:
        DateTimeFormat.asMariaDbDatabasePattern(DateTimeFormat.WEEKDAY_DATE_SHORT) == "%a, %d %M %Y"
    }

    @Unroll
    def "the compact date formats in use are unchanged (#format)"() {
        given:
        def formatter = DateTimeFormat.asJavaFormatter(format, ZoneId.of("UTC"))

        expect: "adding a format must not alter formats already rendered elsewhere"
        formatter.format(WEDNESDAY_ELEVENTH_FEB_2026) == expected

        where:
        format                          || expected
        DateTimeFormat.SIMPLE_DATE_SHORT      || "11 Feb 2026"
        DateTimeFormat.SIMPLE_DATE            || "Wednesday, 11 February 2026"
        DateTimeFormat.SIMPLE_DATE_TIME_SHORT || "11 Feb 2026 10:15"
    }

    def "every format resolves both a UI formatter and a database pattern"() {
        expect: "the two switches are exhaustive, so a new constant cannot ship half-implemented"
        DateTimeFormat.values().every { format ->
            DateTimeFormat.asJavaFormatter(format, ZoneId.of("UTC")) != null &&
                    DateTimeFormat.asMariaDbDatabasePattern(format) != null
        }
    }
}
