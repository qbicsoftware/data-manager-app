package life.qbic.datamanager.views.projects.project.rawdata.pagination

import life.qbic.application.commons.SortOrder
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataSortingKey
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection
import spock.lang.Specification

class RawDataSortSpec extends Specification {

    def "default sort is measurement id ascending"() {
        expect:
        RawDataSort.DEFAULT == new SortOrder("measurementId", false)
    }

    def "translates a UI SortOrder into the API sort order"() {
        expect:
        RawDataSort.toApiSortOrder(new SortOrder("uploadDate", true))
            == new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(
            RawDataSortingKey.UPLOAD_DATE, SortDirection.DESC)
        RawDataSort.toApiSortOrder(new SortOrder("measurementId", false))
            == new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(
            RawDataSortingKey.MEASUREMENT_ID, SortDirection.ASC)
        RawDataSort.toApiSortOrder(new SortOrder("sampleName", false))
            == new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(
            RawDataSortingKey.SAMPLE_NAME, SortDirection.ASC)
    }

    def "returns an empty API sort order list for a non-sortable property"() {
        expect:
        RawDataSort.toApiSortOrder(new SortOrder("bogusField", true)) == null
        RawDataSort.toApiSortOrders(new SortOrder("bogusField", true)).isEmpty()
    }

    def "toApiSortOrders wraps a single valid order"() {
        expect:
        RawDataSort.toApiSortOrders(new SortOrder("measurementId", true))
            == [new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(
            RawDataSortingKey.MEASUREMENT_ID, SortDirection.DESC)]
    }

    def "isValid accepts only the raw data sort properties"() {
        expect:
        RawDataSort.isValid(new SortOrder("measurementId", true))
        RawDataSort.isValid(new SortOrder("uploadDate", false))
        RawDataSort.isValid(new SortOrder("sampleName", true))
        !RawDataSort.isValid(new SortOrder("bogusField", true))
    }

    def "allowed sorts cover both directions of every raw data sort property"() {
        expect:
        RawDataSort.SORTS.contains(new SortOrder("measurementId", false))
        RawDataSort.SORTS.contains(new SortOrder("measurementId", true))
        RawDataSort.SORTS.contains(new SortOrder("uploadDate", false))
        RawDataSort.SORTS.contains(new SortOrder("sampleName", true))
        RawDataSort.SORTS.size() == 6
    }
}