package life.qbic.datamanager.views.projects.project.measurements.pagination

import com.vaadin.flow.router.QueryParameters
import life.qbic.datamanager.views.general.pagination.ListState
import life.qbic.application.commons.SortOrder
import spock.lang.Specification

class MeasurementListStateCodecSpec extends Specification {

    private static final SortOrder DEFAULT_SORT = new SortOrder("registeredAt", true)

    def "parses defaults when no parameters are present"() {
        given:
        def current = MeasurementListState.defaultWith(MeasurementDomain.NGS)

        when:
        def state = MeasurementListStateCodec.parse(QueryParameters.empty(), current)

        then:
        state.activeTab() == MeasurementDomain.NGS
        state.activeState().page() == 1
        state.activeState().pageSize() == MeasurementListStateDefaults.DEFAULT_PAGE_SIZE
        state.activeState().filter() == ""
        state.activeState().sort() == DEFAULT_SORT
    }

    def "parses the active tab and its list state from the URL"() {
        given:
        def current = MeasurementListState.defaultWith(MeasurementDomain.NGS)
        def params = QueryParameters.fromString("tab=ip&page=3&size=48&q=mhc&sort=mhcAntibody:asc")

        when:
        def state = MeasurementListStateCodec.parse(params, current)

        then:
        state.activeTab() == MeasurementDomain.IP
        state.activeState().page() == 3
        state.activeState().pageSize() == 48
        state.activeState().filter() == "mhc"
        state.stateOf(MeasurementDomain.IP).sort() == new SortOrder("mhcAntibody", false)
    }

    def "keeps the other tabs' in-session state and updates only the active tab from the URL"() {
        given:
        def current = MeasurementListState.defaultWith(MeasurementDomain.NGS)
            .withState(MeasurementDomain.PXP, new ListState(2, 12, "prot", new SortOrder("facility", true)))

        when:
        def state = MeasurementListStateCodec.parse(
            QueryParameters.fromString("tab=pxp&page=1&size=24"), current)

        then:
        state.activeTab() == MeasurementDomain.PXP
        state.stateOf(MeasurementDomain.PXP).page() == 1
        state.stateOf(MeasurementDomain.PXP).pageSize() == 24
        and: "the genomics tab state was not touched by the URL"
        state.stateOf(MeasurementDomain.NGS).page() == 1
    }

    def "falls back to defaults for invalid page, size, and sort values"() {
        given:
        def current = MeasurementListState.defaultWith(MeasurementDomain.NGS)

        when:
        def state = MeasurementListStateCodec.parse(
            QueryParameters.fromString("tab=pxp&page=0&size=7&sort=bogus:desc"), current)

        then:
        state.stateOf(MeasurementDomain.PXP).page() == 1
        state.stateOf(MeasurementDomain.PXP).pageSize() == MeasurementListStateDefaults.DEFAULT_PAGE_SIZE
        state.stateOf(MeasurementDomain.PXP).sort() == DEFAULT_SORT
    }

    def "serialises the active tab and its list state, and parses back to the identical state"() {
        given:
        def current = MeasurementListState.defaultWith(MeasurementDomain.PXP)
            .withState(MeasurementDomain.PXP,
                new ListState(2, 24, "cancer", new SortOrder("measurementCode", true)))

        when:
        def params = MeasurementListStateCodec.toQueryParameters(current)

        then: "the URL carries tab plus the active tab's list state"
        params.getSingleParameter("tab").get() == "pxp"
        params.getSingleParameter("page").get() == "2"
        params.getSingleParameter("size").get() == "24"
        params.getSingleParameter("q").get() == "cancer"
        params.getSingleParameter("sort").get() == "measurementCode:desc"

        and: "round-trips through parse"
        def state = MeasurementListStateCodec.parse(params, MeasurementListState.defaultWith(MeasurementDomain.NGS))
        state.activeTab() == MeasurementDomain.PXP
        state.stateOf(MeasurementDomain.PXP) == new ListState(2, 24, "cancer", new SortOrder("measurementCode", true))
    }

    def "unknown tab value falls back to the ngs tab"() {
        given:
        def current = MeasurementListState.defaultWith(MeasurementDomain.NGS)

        when:
        def state = MeasurementListStateCodec.parse(
            QueryParameters.fromString("tab=unknown&page=2"), current)

        then:
        state.activeTab() == MeasurementDomain.NGS
        state.activeState().page() == 2 // page is still honoured
        state.activeState().sort() == DEFAULT_SORT // unknown sort falls back to default
    }
}