package life.qbic.projectmanagement.application.query

import spock.lang.Specification

import java.time.Instant

/**
 * Pins the semantics of {@link MeasurementDataRow}: it carries both process steps of a measurement
 * entry and states which one it is in.
 *
 * <p>The distinction the page depends on is "no datum" versus "a datum of zero bytes". The external
 * sync does not always record sizes, so {@code 0} is ambiguous and must not be rendered as an empty
 * dataset — these tests pin that the two cases stay distinguishable.</p>
 */
class MeasurementDataRowSpec extends Specification {

    private static final Instant REGISTERED = Instant.parse("2026-01-12T10:15:30Z")
    private static final Instant UPLOADED = Instant.parse("2026-01-14T08:00:00Z")

    private static MeasurementDataRow waiting() {
        return new MeasurementDataRow("mid-1", "exp-1", "GENOMICS", "NGS-123", "NGS run",
                "QBiC", "{\"label\":\"NovaSeq\"}", "pool-1", 2, REGISTERED,
                MeasurementDataRow.DataStatus.WAITING, null, null, null, null)
    }

    private static MeasurementDataRow available(Long files, Long bytes) {
        return new MeasurementDataRow("mid-2", "exp-1", "PROTEOMICS", "MS-456", "PX run",
                "QBiC", null, "pool-2", 3, REGISTERED,
                MeasurementDataRow.DataStatus.AVAILABLE, files, bytes,
                Set.of("raw", "mzml"), UPLOADED)
    }

    def "a waiting entry has no data and reports no size"() {
        when:
        def row = waiting()

        then:
        !row.hasData()
        row.dataStatus() == MeasurementDataRow.DataStatus.WAITING
        !row.hasRecordedSize()
        row.dataSizeBytes() == null
        row.dataUploadedAt() == null
        row.dataFileTypes().isEmpty()
    }

    def "an available entry reports its data facts"() {
        when:
        def row = available(3L, 4_200_000_000L)

        then:
        row.hasData()
        row.hasRecordedSize()
        row.fileCountOrZero() == 3
        row.dataFileTypes() == Set.of("raw", "mzml")
        row.dataUploadedAt() == UPLOADED
    }

    def "a datum whose size the sync never recorded is available but states no size"() {
        when: "the sync reported files but no bytes (0 is the ambiguous value)"
        def row = available(3L, 0L)

        then: "it is available — data exists"
        row.hasData()

        and: "but the size is not asserted, so the page does not print '0 B'"
        !row.hasRecordedSize()
    }

    def "an available entry without a recorded file count reports zero rather than failing"() {
        when:
        def row = available(null, 1234L)

        then:
        row.hasData()
        row.fileCountOrZero() == 0
    }

    def "file types default to empty instead of null so callers never null-check"() {
        when:
        def row = new MeasurementDataRow("mid-3", "exp-1", "IMMUNOPEPTIDOMICS", "IP-789", null,
                null, null, null, 0, REGISTERED, MeasurementDataRow.DataStatus.WAITING,
                null, null, null, null)

        then:
        row.dataFileTypes() != null
        row.dataFileTypes().isEmpty()
    }

    def "the measurement identity is required"() {
        when:
        new MeasurementDataRow(null, "exp-1", "GENOMICS", "NGS-1", null, null, null, null, 0,
                REGISTERED, MeasurementDataRow.DataStatus.WAITING, null, null, null, null)

        then:
        thrown(NullPointerException)
    }

    def "the data status is required so an entry is never neither waiting nor available"() {
        when:
        new MeasurementDataRow("mid-4", "exp-1", "GENOMICS", "NGS-1", null, null, null, null, 0,
                REGISTERED, null, null, null, null, null)

        then:
        thrown(NullPointerException)
    }
}
