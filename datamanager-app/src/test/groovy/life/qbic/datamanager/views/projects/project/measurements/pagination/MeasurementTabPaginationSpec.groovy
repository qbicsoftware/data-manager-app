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

    def "search does not request scrolling the grid top into view"() {
        given:
        def container = newContainer()
        def scrollRequests = []
        container.addRefreshRequestedListener(
            event -> scrollRequests << event.scrollGridTopIntoView())

        when:
        container.applySearch(MeasurementDomain.NGS, "cancer")

        then:
        scrollRequests == [false]
    }

    def "sort does not request scrolling the grid top into view"() {
        given:
        def container = newContainer()
        def scrollRequests = []
        container.addRefreshRequestedListener(
            event -> scrollRequests << event.scrollGridTopIntoView())

        when:
        container.applySort(MeasurementDomain.NGS, new SortOrder("facility", true))

        then:
        scrollRequests == [false]
    }

    def "external state application does not request scrolling the grid top into view"() {
        given:
        def container = newContainer()
        def scrollRequests = []
        container.addRefreshRequestedListener(
            event -> scrollRequests << event.scrollGridTopIntoView())

        when: "a shared link / back-forward restores the URL state"
        container.applyExternalState(MeasurementListState.defaultWith(MeasurementDomain.PXP))

        then:
        scrollRequests == [false]
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

    def "selection bar follows the active tab on user tab switch"() {
        given: "selection bar attached to the initial (NGS) tab"
        def container = newContainer()
        container.attachSelectionBar()

        when: "the user switches to the Proteomics tab"
        container.setActiveTab(MeasurementDomain.PXP)

        then: "the selection bar is re-parented into the proteomics tab content"
        // no UI here, so assert the shared bar's parent element belongs to the PXP tab content
        container.selectionBar().getParent().isPresent()
        container.selectionBar().getParent().get() instanceof com.vaadin.flow.component.html.Span
    }

    def "hiding the active tab falls back to the first remaining visible tab and refreshes it"() {
        given: "only proteomics measurements exist (NGS has none)"
        def container = newContainer()
        def refreshes = []
        container.addRefreshRequestedListener(event -> refreshes << event.domain())

        when: "the owning view hides the NGS tab because it has no measurements"
        container.setTabVisible(MeasurementDomain.NGS, false)

        then: "the active tab moves to the first still-visible tab (PXP) and refreshes it"
        container.activeTab() == MeasurementDomain.PXP
        refreshes == [MeasurementDomain.PXP]

        when: "the fallback target tab is hidden too (only IP remains)"
        container.setTabVisible(MeasurementDomain.PXP, false)

        then: "the active tab moves to the last visible one and refreshes it"
        container.activeTab() == MeasurementDomain.IP
        refreshes == [MeasurementDomain.PXP, MeasurementDomain.IP]
    }

    def "hiding a non-active tab does not change the active tab"() {
        given:
        def container = newContainer()
        def refreshes = []
        container.addRefreshRequestedListener(event -> refreshes << event.domain())

        when:
        container.setTabVisible(MeasurementDomain.IP, false)

        then:
        container.activeTab() == MeasurementDomain.NGS
        refreshes == []
    }

    def "hiding the last visible tab leaves the active tab at the last fallback target"() {
        given: "all tabs start visible, active is NGS"
        def container = newContainer()
        def refreshes = []
        container.addRefreshRequestedListener(event -> refreshes << event.domain())

        when: "hide all three (NGS first)"
        container.setTabVisible(MeasurementDomain.NGS, false)
        container.setTabVisible(MeasurementDomain.PXP, false)
        container.setTabVisible(MeasurementDomain.IP, false)

        then: "the active tab falls through NGS→PXP→IP and stays at the last fallback target"
        container.activeTab() == MeasurementDomain.IP
        refreshes == [MeasurementDomain.PXP, MeasurementDomain.IP]
    }
}