package life.qbic.datamanager.views.projects.project.samples

import life.qbic.application.commons.SortOrder
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewSortKey
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pins the sort translation and validation for the paginated sample list (FEAT-PAG-LIST-02).
 */
class SampleSortSpec extends Specification {

    def "default sort is sample id ascending"() {
        expect:
        SampleSort.DEFAULT == new SortOrder("sampleId", false)
        SampleSort.DEFAULT.isAscending()
    }

    def "translates a sort order to the API sort key and direction"() {
        expect:
        SampleSort.toApiSortOrder(sortOrder) == new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(key, direction)

        where:
        sortOrder                                 | key                           | direction
        new SortOrder("sampleId", false)          | SamplePreviewSortKey.SAMPLE_ID | SortDirection.ASC
        new SortOrder("sampleName", true)         | SamplePreviewSortKey.SAMPLE_NAME | SortDirection.DESC
        new SortOrder("biologicalReplicate", false) | SamplePreviewSortKey.BIOLOGICAL_REPLICATE | SortDirection.ASC
        new SortOrder("batch", false)             | SamplePreviewSortKey.BATCH | SortDirection.ASC
        new SortOrder("condition", true)          | SamplePreviewSortKey.CONDITION | SortDirection.DESC
        new SortOrder("species", false)           | SamplePreviewSortKey.SPECIES | SortDirection.ASC
        new SortOrder("specimen", false)          | SamplePreviewSortKey.SPECIMEN | SortDirection.ASC
        new SortOrder("analyte", false)           | SamplePreviewSortKey.ANALYTE | SortDirection.ASC
        new SortOrder("analysisMethod", false)    | SamplePreviewSortKey.ANALYSIS_METHOD | SortDirection.ASC
        new SortOrder("comment", false)           | SamplePreviewSortKey.COMMENT | SortDirection.ASC
        new SortOrder("registrationTime", true)   | SamplePreviewSortKey.REGISTRATION_TIME | SortDirection.DESC
        new SortOrder("lastModified", false)      | SamplePreviewSortKey.MODIFICATION_TIME | SortDirection.ASC
    }

    def "rejects an unknown sort property"() {
        when:
        SampleSort.toApiSortOrder(new SortOrder("notAProperty", false))

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "allowed sort orders contains #property #direction"() {
        expect:
        SampleSort.allowedSortOrders().contains(new SortOrder(property, direction))

        where:
        property        | direction
        "sampleId"      | false
        "sampleId"      | true
        "sampleName"    | false
        "batch"         | false
        "lastModified"  | true
    }

    def "null sort order is rejected"() {
        when:
        SampleSort.toApiSortOrder(null)

        then:
        thrown(NullPointerException)
    }
}