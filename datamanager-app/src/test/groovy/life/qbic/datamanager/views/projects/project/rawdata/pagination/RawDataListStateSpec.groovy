package life.qbic.datamanager.views.projects.project.rawdata.pagination

import life.qbic.application.commons.SortOrder
import life.qbic.datamanager.views.general.pagination.ListState
import spock.lang.Specification

class RawDataListStateSpec extends Specification {

    private static final SortOrder DEFAULT_SORT = new SortOrder("measurementId", false)

    def "default state uses ngs tab, page 1, default size, measurementId asc"() {
        when:
        def state = RawDataListState.defaultWith(RawDataDomain.NGS)

        then:
        state.activeTab() == RawDataDomain.NGS
        state.activeState().page() == 1
        state.activeState().pageSize() == RawDataListStateDefaults.DEFAULT_PAGE_SIZE
        state.activeState().filter() == ""
        state.activeState().sort() == DEFAULT_SORT
    }

    def "stateOf returns the per-domain state and withState replaces it"() {
        given:
        def ngs = new ListState(3, 24, "cancer", new SortOrder("uploadDate", true))
        def state = RawDataListState.defaultWith(RawDataDomain.PXP).withState(RawDataDomain.NGS, ngs)

        expect:
        state.stateOf(RawDataDomain.NGS) == ngs
        state.activeState() == state.stateOf(RawDataDomain.PXP)
    }

    def "withTab switches the active tab and keeps per-tab states"() {
        given:
        def ip = new ListState(2, 48, "mhc", new SortOrder("sampleName", true))
        def state = RawDataListState.defaultWith(RawDataDomain.NGS)
            .withState(RawDataDomain.IP, ip)
            .withTab(RawDataDomain.IP, ip)

        expect:
        state.activeTab() == RawDataDomain.IP
        state.activeState() == ip
        state.stateOf(RawDataDomain.NGS).page() == 1
    }

    def "defaultStateFor returns the same defaults for each domain"() {
        expect:
        RawDataListState.defaultStateFor(RawDataDomain.NGS)
            == RawDataListState.defaultStateFor(RawDataDomain.IP)
    }

    def "null per-tab states fall back to defaults"() {
        when:
        def state = RawDataListState.defaultWith(RawDataDomain.PXP)

        then:
        state.stateOf(RawDataDomain.NGS).page() == 1
        state.stateOf(RawDataDomain.IP).page() == 1
    }
}