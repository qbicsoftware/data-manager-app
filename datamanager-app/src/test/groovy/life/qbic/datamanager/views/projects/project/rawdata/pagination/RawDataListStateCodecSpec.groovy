package life.qbic.datamanager.views.projects.project.rawdata.pagination

import com.vaadin.flow.router.QueryParameters
import life.qbic.application.commons.SortOrder
import life.qbic.datamanager.views.general.pagination.ListState
import spock.lang.Specification

class RawDataListStateCodecSpec extends Specification {

    private static final SortOrder DEFAULT_SORT = new SortOrder("measurementId", false)

    def "parses defaults when no parameters are present"() {
        given:
        def current = RawDataListState.defaultWith(RawDataDomain.NGS)

        when:
        def state = RawDataListStateCodec.parse(QueryParameters.empty(), current)

        then:
        state.activeTab() == RawDataDomain.NGS
        state.activeState().page() == 1
        state.activeState().pageSize() == RawDataListStateDefaults.DEFAULT_PAGE_SIZE
        state.activeState().filter() == ""
        state.activeState().sort() == DEFAULT_SORT
    }

    def "parses the active tab and its list state from the URL"() {
        given:
        def current = RawDataListState.defaultWith(RawDataDomain.NGS)
        def params = QueryParameters.fromString("tab=ip&page=3&size=48&q=mhc&sort=uploadDate:desc")

        when:
        def state = RawDataListStateCodec.parse(params, current)

        then:
        state.activeTab() == RawDataDomain.IP
        state.activeState().page() == 3
        state.activeState().pageSize() == 48
        state.activeState().filter() == "mhc"
        state.stateOf(RawDataDomain.IP).sort() == new SortOrder("uploadDate", true)
    }

    def "keeps the other tabs' in-session state and updates only the active tab from the URL"() {
        given:
        def current = RawDataListState.defaultWith(RawDataDomain.NGS)
            .withState(RawDataDomain.PXP,
                new ListState(2, 12, "prot", new SortOrder("uploadDate", true)))

        when:
        def state = RawDataListStateCodec.parse(
            QueryParameters.fromString("tab=pxp&page=1&size=24"), current)

        then:
        state.activeTab() == RawDataDomain.PXP
        state.stateOf(RawDataDomain.PXP).page() == 1
        state.stateOf(RawDataDomain.PXP).pageSize() == 24
        and: "the genomics tab state was not touched by the URL"
        state.stateOf(RawDataDomain.NGS).page() == 1
    }

    def "falls back to defaults for invalid page, size, and sort values"() {
        given:
        def current = RawDataListState.defaultWith(RawDataDomain.NGS)

        when:
        def state = RawDataListStateCodec.parse(
            QueryParameters.fromString("tab=pxp&page=0&size=7&sort=bogus:desc"), current)

        then:
        state.stateOf(RawDataDomain.PXP).page() == 1
        state.stateOf(RawDataDomain.PXP).pageSize() == RawDataListStateDefaults.DEFAULT_PAGE_SIZE
        state.stateOf(RawDataDomain.PXP).sort() == DEFAULT_SORT
    }

    def "serialises the active tab and its list state, and parses back to the identical state"() {
        given:
        def current = RawDataListState.defaultWith(RawDataDomain.PXP)
            .withState(RawDataDomain.PXP,
                new ListState(2, 24, "cancer", new SortOrder("uploadDate", true)))

        when:
        def params = RawDataListStateCodec.toQueryParameters(current)

        then: "the URL carries tab plus the active tab's list state"
        params.getSingleParameter("tab").get() == "pxp"
        params.getSingleParameter("page").get() == "2"
        params.getSingleParameter("size").get() == "24"
        params.getSingleParameter("q").get() == "cancer"
        params.getSingleParameter("sort").get() == "uploadDate:desc"

        and: "round-trips through parse"
        def state = RawDataListStateCodec.parse(params,
            RawDataListState.defaultWith(RawDataDomain.NGS))
        state.activeTab() == RawDataDomain.PXP
        state.stateOf(RawDataDomain.PXP) == new ListState(2, 24, "cancer",
            new SortOrder("uploadDate", true))
    }

    def "unknown tab value falls back to the ngs tab"() {
        given:
        def current = RawDataListState.defaultWith(RawDataDomain.NGS)

        when:
        def state = RawDataListStateCodec.parse(
            QueryParameters.fromString("tab=unknown&page=2"), current)

        then:
        state.activeTab() == RawDataDomain.NGS
        state.activeState().page() == 2 // page is still honoured
        state.activeState().sort() == DEFAULT_SORT // unknown sort falls back to default
    }
}