package life.qbic.datamanager.views.projects.project.rawdata.pagination

import com.vaadin.flow.component.html.Span
import life.qbic.application.commons.SortOrder
import life.qbic.datamanager.views.general.pagination.ListState
import life.qbic.datamanager.views.general.pagination.Selection
import spock.lang.Specification

/**
 * Pins the state-transition semantics of the tabbed raw data pagination container
 * (FEAT-PAG-LIST-04): per-tab list state, external (URL) application, search/sort state
 * updates and page clamping. The container is a pure view coordinator — no UI is required
 * because it never queries data itself and writes to the URL only when a UI is present.
 */
class RawDataTabPaginationSpec extends Specification {

    private static final SortOrder DEFAULT_SORT = new SortOrder("measurementId", false)

    RawDataTabPagination newContainer() {
        def container = new RawDataTabPagination()
        container.addTab("Genomics", RawDataDomain.NGS, new Span())
        container.addTab("Proteomics", RawDataDomain.PXP, new Span())
        container.addTab("Immunopeptidomics", RawDataDomain.IP, new Span())
        return container
    }

    def "defaults to the NGS tab with the raw data default page size"() {
        given:
        def container = newContainer()

        expect:
        container.activeTab() == RawDataDomain.NGS
        container.listState().activeState().page() == 1
        container.listState().activeState().pageSize() == RawDataListStateDefaults.DEFAULT_PAGE_SIZE
        container.listState().activeState().filter() == ""
        container.listState().activeState().sort() == DEFAULT_SORT
    }

    def "applying an external state switches the active tab and keeps inactive tab states"() {
        given:
        def container = newContainer()
        def external = RawDataListState.defaultWith(RawDataDomain.PXP)
            .withState(RawDataDomain.PXP,
                new ListState(3, RawDataListStateDefaults.DEFAULT_PAGE_SIZE, "prot",
                    new SortOrder("uploadDate", true)))
        int refreshes = 0
        container.addRefreshRequestedListener(event -> refreshes++)

        when:
        container.applyExternalState(external)

        then: "the active tab follows the external state"
        container.activeTab() == RawDataDomain.PXP
        container.listState().stateOf(RawDataDomain.PXP).page() == 3
        container.listState().stateOf(RawDataDomain.PXP).filter() == "prot"

        and: "the inactive tabs keep their (default) state"
        container.listState().stateOf(RawDataDomain.NGS).page() == 1

        and: "applyExternalState requests a refetch of the new active tab"
        refreshes == 1
    }

    def "applying a search term resets to page one of the given tab"() {
        given:
        def container = newContainer()
        container.applyExternalState(RawDataListState.defaultWith(RawDataDomain.PXP)
            .withState(RawDataDomain.PXP,
                new ListState(4, RawDataListStateDefaults.DEFAULT_PAGE_SIZE, "",
                    DEFAULT_SORT)))

        when:
        container.applySearch(RawDataDomain.PXP, "cancer")

        then:
        container.listState().stateOf(RawDataDomain.PXP).filter() == "cancer"
        container.listState().stateOf(RawDataDomain.PXP).page() == 1
    }

    def "search does not request scrolling the grid top into view"() {
        given:
        def container = newContainer()
        def scrollRequests = []
        container.addRefreshRequestedListener(
            event -> scrollRequests << event.scrollGridTopIntoView())

        when:
        container.applySearch(RawDataDomain.NGS, "cancer")

        then:
        scrollRequests == [false]
    }

    def "applying the same search term again is a no-op"() {
        given:
        def container = newContainer()
        container.applySearch(RawDataDomain.NGS, "cancer")
        def refreshes = 0
        container.addRefreshRequestedListener(event -> refreshes++)

        when:
        container.applySearch(RawDataDomain.NGS, "cancer")

        then:
        container.listState().activeState().filter() == "cancer"
        refreshes == 0
    }

    def "applying a sort order updates the given tab and resets its page"() {
        given:
        def container = newContainer()
        container.applyExternalState(RawDataListState.defaultWith(RawDataDomain.IP)
            .withState(RawDataDomain.IP,
                new ListState(2, RawDataListStateDefaults.DEFAULT_PAGE_SIZE, "",
                    DEFAULT_SORT)))

        when:
        container.applySort(RawDataDomain.IP, new SortOrder("sampleName", true))

        then:
        container.listState().stateOf(RawDataDomain.IP).sort() == new SortOrder("sampleName", true)
        container.listState().stateOf(RawDataDomain.IP).page() == 1
    }

    def "onPageLoaded stores the rendered page and total for the active tab"() {
        given:
        def container = newContainer()
        container.setSelection(new Selection(null as Runnable))

        when: "the owning view reports the (already clamped) rendered page"
        container.onPageLoaded(RawDataDomain.NGS, 2, 25L)

        then: "the container stores the reported page"
        container.listState().activeState().page() == 2
        container.listState().activeState().pageSize() == RawDataListStateDefaults.DEFAULT_PAGE_SIZE
    }

    def "tab switch via setActiveTab requests a refresh for the switched-to tab"() {
        given:
        def container = newContainer()
        def domains = []
        container.addRefreshRequestedListener(event -> domains << event.domain())

        when:
        container.setActiveTab(RawDataDomain.IP)

        then:
        container.activeTab() == RawDataDomain.IP
        domains == [RawDataDomain.IP]
    }

    def "selection bar follows the active tab on user tab switch"() {
        given: "selection bar attached to the initial (NGS) tab"
        def container = newContainer()
        container.attachSelectionBar()

        when: "the user switches to the Proteomics tab"
        container.setActiveTab(RawDataDomain.PXP)

        then: "the selection bar is re-parented into the proteomics tab content"
        container.selectionBar().getParent().isPresent()
        container.selectionBar().getParent().get() instanceof com.vaadin.flow.component.html.Span
    }

    def "hiding the active tab falls back to the first remaining visible tab and refreshes it"() {
        given: "only proteomics raw data exists (NGS has none)"
        def container = newContainer()
        def refreshes = []
        container.addRefreshRequestedListener(event -> refreshes << event.domain())

        when: "the owning view hides the NGS tab because it has no raw data"
        container.setTabVisible(RawDataDomain.NGS, false)

        then: "the active tab moves to the first still-visible tab (PXP) and refreshes it"
        container.activeTab() == RawDataDomain.PXP
        refreshes == [RawDataDomain.PXP]

        when: "the fallback target tab is hidden too (only IP remains)"
        container.setTabVisible(RawDataDomain.PXP, false)

        then: "the active tab moves to the last visible one and refreshes it"
        container.activeTab() == RawDataDomain.IP
        refreshes == [RawDataDomain.PXP, RawDataDomain.IP]
    }

    def "hiding a non-active tab does not change the active tab"() {
        given:
        def container = newContainer()
        def refreshes = []
        container.addRefreshRequestedListener(event -> refreshes << event.domain())

        when:
        container.setTabVisible(RawDataDomain.IP, false)

        then:
        container.activeTab() == RawDataDomain.NGS
        refreshes == []
    }

    def "hiding the last visible tab leaves the active tab at the last fallback target"() {
        given: "all tabs start visible, active is NGS"
        def container = newContainer()
        def refreshes = []
        container.addRefreshRequestedListener(event -> refreshes << event.domain())

        when: "hide all three (NGS first)"
        container.setTabVisible(RawDataDomain.NGS, false)
        container.setTabVisible(RawDataDomain.PXP, false)
        container.setTabVisible(RawDataDomain.IP, false)

        then: "the active tab falls through NGS→PXP→IP and stays at the last fallback target"
        container.activeTab() == RawDataDomain.IP
        refreshes == [RawDataDomain.PXP, RawDataDomain.IP]
    }
}