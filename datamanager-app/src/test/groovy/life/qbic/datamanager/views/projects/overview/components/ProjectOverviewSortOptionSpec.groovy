package life.qbic.datamanager.views.projects.overview.components

import life.qbic.application.commons.SortOrder
import spock.lang.Specification

class ProjectOverviewSortOptionSpec extends Specification {

    def "default sort is last modified descending"() {
        expect:
        ProjectOverviewSortOption.defaultSort() == new SortOrder("lastModified", true)
    }

    def "sort options map to the service sort orders"() {
        expect:
        ProjectOverviewSortOption.LAST_MODIFIED_DESC.toSortOrder() == new SortOrder("lastModified", true)
        ProjectOverviewSortOption.TITLE_ASC.toSortOrder() == new SortOrder("projectTitle", false)
        ProjectOverviewSortOption.TITLE_DESC.toSortOrder() == new SortOrder("projectTitle", true)
        ProjectOverviewSortOption.CODE_ASC.toSortOrder() == new SortOrder("projectCode", false)
        ProjectOverviewSortOption.CODE_DESC.toSortOrder() == new SortOrder("projectCode", true)
    }

    def "allowed sort orders are the selectable options"() {
        expect:
        ProjectOverviewSortOption.allowedSortOrders().size() == ProjectOverviewSortOption.values().size()
    }

    def "unknown sort orders resolve to the default option"() {
        expect:
        ProjectOverviewSortOption.fromSortOrder(new SortOrder("bogusField", true)) == ProjectOverviewSortOption.LAST_MODIFIED_DESC
    }
}