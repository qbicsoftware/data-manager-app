package life.qbic.datamanager.views.projects.project.measurements.pagination

import life.qbic.application.commons.SortOrder
import life.qbic.datamanager.views.general.pagination.ListState
import spock.lang.Specification

class MeasurementListStateSpec extends Specification {

    private static final SortOrder DEFAULT_SORT = new SortOrder("registeredAt", true)

    def "default state uses ngs tab, page 1, default size, registeredAt desc"() {
        when:
        def state = MeasurementListState.defaultWith(MeasurementDomain.NGS)

        then:
        state.activeTab() == MeasurementDomain.NGS
        state.activeState().page() == 1
        state.activeState().pageSize() == MeasurementListStateDefaults.DEFAULT_PAGE_SIZE
        state.activeState().filter() == ""
        state.activeState().sort() == DEFAULT_SORT
    }

    def "stateOf returns the per-domain state and withState replaces it"() {
        given:
        def ngs = new ListState(3, 24, "cancer", new SortOrder("facility", true))
        def state = MeasurementListState.defaultWith(MeasurementDomain.PXP).withState(MeasurementDomain.NGS, ngs)

        expect:
        state.stateOf(MeasurementDomain.NGS) == ngs
        state.activeState() == state.stateOf(MeasurementDomain.PXP)
    }

    def "withTab switches the active tab and keeps per-tab states"() {
        given:
        def ip = new ListState(2, 48, "mhc", new SortOrder("mhcAntibody", true))
        def state = MeasurementListState.defaultWith(MeasurementDomain.NGS)
            .withState(MeasurementDomain.IP, ip)
            .withTab(MeasurementDomain.IP, ip)

        expect:
        state.activeTab() == MeasurementDomain.IP
        state.activeState() == ip
        state.stateOf(MeasurementDomain.NGS).page() == 1
    }

    def "defaultStateFor returns the same defaults for each domain"() {
        expect:
        MeasurementListState.defaultStateFor(MeasurementDomain.NGS)
            == MeasurementListState.defaultStateFor(MeasurementDomain.IP)
    }

    def "null per-tab states fall back to defaults"() {
        when:
        def state = MeasurementListState.defaultWith(MeasurementDomain.PXP)

        then:
        state.stateOf(MeasurementDomain.NGS).page() == 1
        state.stateOf(MeasurementDomain.IP).page() == 1
    }
}