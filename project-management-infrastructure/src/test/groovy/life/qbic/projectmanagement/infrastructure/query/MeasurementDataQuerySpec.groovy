package life.qbic.projectmanagement.infrastructure.query

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Guards the two properties of the Measurement Dataset read model that cannot fail loudly at
 * runtime but silently produce a lying page:
 *
 * <ol>
 *   <li>The datum must be joined with a {@code LEFT JOIN}. The existing raw-data views use an
 *       {@code INNER JOIN}, which makes a measurement whose data has not arrived yet a missing row —
 *       exactly the normal post-registration state. A regression to an inner join would make every
 *       entry read as "available" and nothing would throw.</li>
 *   <li>Page and count must be filtered identically, or the pager reports a total that does not
 *       match its own content.</li>
 * </ol>
 */
class MeasurementDataQuerySpec extends Specification {

    def "the datum is joined with a LEFT JOIN so a waiting entry is still a row"() {
        when:
        def sql = MeasurementDataQuery.page(MeasurementDataQuery.orderBy(
                MeasurementDataQuery.SortColumn.MEASUREMENT_ID, true))

        then:
        sql.contains("LEFT JOIN remote_measurement_data")
        !sql.contains("INNER JOIN remote_measurement_data")
    }

    def "a deleted datum is reported as waiting, never as available"() {
        when:
        def sql = MeasurementDataQuery.page(MeasurementDataQuery.orderBy(
                MeasurementDataQuery.SortColumn.MEASUREMENT_ID, true))

        then: "the status derives from the deleted flag rather than from the row's existence"
        sql.contains("CASE WHEN rmd.deleted = FALSE THEN 'AVAILABLE' ELSE 'WAITING' END")
    }

    def "page and count are built from the same entries CTE and the same filter"() {
        when:
        def page = MeasurementDataQuery.page(MeasurementDataQuery.orderBy(
                MeasurementDataQuery.SortColumn.MEASUREMENT_ID, true))
        def count = MeasurementDataQuery.count()

        then: "both scope measurements to the experiment through their samples"
        page.contains("JOIN experiment_samples es")
        count.contains("JOIN experiment_samples es")

        and: "both filter on type, search term and data status"
        [page, count].each { sql ->
            assert sql.contains(":measurementType")
            assert sql.contains(":searchPattern")
            assert sql.contains(":dataStatus")
        }
    }

    def "the count applies no sorting and no pagination of its own"() {
        when:
        def count = MeasurementDataQuery.count()

        then: "no statement-level ORDER BY / LIMIT (the CTE may order inside GROUP_CONCAT)"
        !count.contains("ORDER BY measurement_")
        !count.contains("ORDER BY registered_at")
        !count.contains("ORDER BY data_status")
        !count.contains("LIMIT")
    }

    def "the page applies a deterministic tie-break on the measurement code"() {
        expect:
        MeasurementDataQuery.orderBy(MeasurementDataQuery.SortColumn.MEASUREMENT_NAME, true) ==
                "measurement_name ASC, measurement_code ASC"
        MeasurementDataQuery.orderBy(MeasurementDataQuery.SortColumn.DATA_STATUS, false) ==
                "data_status DESC, measurement_code ASC"
        MeasurementDataQuery.orderBy(MeasurementDataQuery.SortColumn.MEASUREMENT_ID, false) ==
                "measurement_code DESC"
    }

    @Unroll
    def "sort column #column resolves to its own SQL fragment"() {
        expect:
        MeasurementDataQuery.SortColumn.valueOf(column.name()).sql() == sql

        where:
        column                                            | sql
        MeasurementDataQuery.SortColumn.MEASUREMENT_ID     | "measurement_code"
        MeasurementDataQuery.SortColumn.MEASUREMENT_NAME   | "measurement_name"
        MeasurementDataQuery.SortColumn.REGISTRATION_DATE  | "registered_at"
        MeasurementDataQuery.SortColumn.DATA_STATUS        | "data_status"
        MeasurementDataQuery.SortColumn.SAMPLE_POOL        | "sample_pool"
    }

    def "every string comparison pins a collation, because instances mix collations"() {
        when:
        def sql = MeasurementDataQuery.page(MeasurementDataQuery.orderBy(
                MeasurementDataQuery.SortColumn.MEASUREMENT_ID, true))

        then: "the experiment scoping, the datum join and the filter all pin utf8mb4_unicode_ci"
        sql.contains("s.experiment_id COLLATE utf8mb4_unicode_ci = :experimentId")
        sql.contains("entries.measurement_code")
        sql.contains("COLLATE utf8mb4_unicode_ci")
        sql.contains("measurement_type COLLATE utf8mb4_unicode_ci = :measurementType")
        sql.contains("data_status COLLATE utf8mb4_unicode_ci = :dataStatus")
    }
}
