package life.qbic.datamanager.views.projects.project.measurements.pagination

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Span
import life.qbic.application.commons.SortOrder
import life.qbic.datamanager.views.general.pagination.ListState
import spock.lang.Specification

/**
 * Pins the state-transition semantics of the tabbed pagination container (ADR-0009):
 * per-tab list state, external (URL) application, search/sort state updates and page
 * clamping. The container is a pure view coordinator — no UI is required because it
 * never queries data itself and writes to the URL only when a UI is present.
 */
class MeasurementTabPaginationSpec extends Specification {

    private static final SortOrder DEFAULT_SORT = new SortOrder("registeredAt", true)

    MeasurementTabPagination newContainer() {
        def container = new MeasurementTabPagination()
        container.addTab("Genomics", MeasurementDomain.NGS, new Span())
        container.addTab("Proteomics", MeasurementDomain.PXP, new Span())
        container.addTab("Immunopeptidomics", MeasurementDomain.IP, new Span())
        return container
    }

    def "defaults to the NGS tab with the measurement default page size"() {
        given:
        def container = newContainer()

        expect:
        container.activeTab() == MeasurementDomain.NGS
        container.listState().activeState().page() == 1
        container.listState().activeState().pageSize() == MeasurementListStateDefaults.DEFAULT_PAGE_SIZE
        container.listState().activeState().filter() == ""
        container.listState().activeState().sort() == DEFAULT_SORT
    }

    def "applying an external state switches the active tab and keeps inactive tab states"() {
        given:
        def container = newContainer()
        def external = MeasurementListState.defaultWith(MeasurementDomain.PXP)
            .withState(MeasurementDomain.PXP,
                new ListState(3, MeasurementListStateDefaults.DEFAULT_PAGE_SIZE, "prot",
                    new SortOrder("facility", true)))
        int refreshes = 0
        container.addRefreshRequestedListener(event -> refreshes++)

        when:
        container.applyExternalState(external)

        then: "the active tab follows the external state"
        container.activeTab() == MeasurementDomain.PXP
        container.listState().stateOf(MeasurementDomain.PXP).page() == 3
        container.listState().stateOf(MeasurementDomain.PXP).filter() == "prot"

        and: "the inactive tabs keep their (default) state"
        container.listState().stateOf(MeasurementDomain.NGS).page() == 1

        and: "applyExternalState requests a refetch of the new active tab"
        refreshes == 1
    }

    def "applying a search term resets to page one of the given tab"() {
        given:
        def container = newContainer()
        container.applyExternalState(MeasurementListState.defaultWith(MeasurementDomain.PXP)
            .withState(MeasurementDomain.PXP,
                new ListState(4, MeasurementListStateDefaults.DEFAULT_PAGE_SIZE, "",
                    DEFAULT_SORT)))

        when:
        container.applySearch(MeasurementDomain.PXP, "cancer")

        then:
        container.listState().stateOf(MeasurementDomain.PXP).filter() == "cancer"
        container.listState().stateOf(MeasurementDomain.PXP).page() == 1
    }

    def "applying the same search term again is a no-op"() {
        given:
        def container = newContainer()
        container.applySearch(MeasurementDomain.NGS, "cancer")
        def refreshes = 0
        container.addRefreshRequestedListener(event -> refreshes++)

        when:
        container.applySearch(MeasurementDomain.NGS, "cancer")

        then:
        container.listState().activeState().filter() == "cancer"
        refreshes == 0
    }

    def "applying a sort order updates the given tab and resets its page"() {
        given:
        def container = newContainer()
        container.applyExternalState(MeasurementListState.defaultWith(MeasurementDomain.IP)
            .withState(MeasurementDomain.IP,
                new ListState(2, MeasurementListStateDefaults.DEFAULT_PAGE_SIZE, "",
                    DEFAULT_SORT)))

        when:
        container.applySort(MeasurementDomain.IP, new SortOrder("mhcAntibody", true))

        then:
        container.listState().stateOf(MeasurementDomain.IP).sort() == new SortOrder("mhcAntibody", true)
        container.listState().stateOf(MeasurementDomain.IP).page() == 1
    }

    def "onPageLoaded stores the rendered page and total for the active tab"() {
        given:
        def container = newContainer()
        container.setSelection(new MeasurementSelection(null as Runnable))

        when: "the owning view reports the (already clamped) rendered page"
        container.onPageLoaded(MeasurementDomain.NGS, 2, 25L)

        then: "the container stores the reported page"
        container.listState().activeState().page() == 2
        container.listState().activeState().pageSize() == MeasurementListStateDefaults.DEFAULT_PAGE_SIZE
    }

    def "tab switch via setActiveTab requests a refresh for the switched-to tab"() {
        given:
        def container = newContainer()
        def domains = []
        container.addRefreshRequestedListener(event -> domains << event.domain())

        when:
        container.setActiveTab(MeasurementDomain.IP)

        then:
        container.activeTab() == MeasurementDomain.IP
        domains == [MeasurementDomain.IP]
    }
}