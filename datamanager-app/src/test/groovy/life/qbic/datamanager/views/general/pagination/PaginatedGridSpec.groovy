package life.qbic.datamanager.views.general.pagination

import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.GridSortOrder
import com.vaadin.flow.data.provider.SortDirection
import life.qbic.application.commons.SortOrder
import spock.lang.Specification

class PaginatedGridSpec extends Specification {

    static final SortOrder DEFAULT_SORT = new SortOrder("code", false)

    /**
     * A tiny fake item with an id.
     */
    static class Item {
        final String id
        Item(String id) { this.id = id }
    }

    /**
     * A page loader backed by an in-memory list, slicing by offset/limit.
     */
    static class FakeLoader implements PaginatedGrid.PageLoader<Item> {
        final List<Item> all
        int loadCount = 0
        FakeLoader(List<Item> all) { this.all = all }

        @Override
        PaginatedGrid.Page<Item> load(ListState state) {
            loadCount++
            int offset = (state.page() - 1) * state.pageSize()
            int end = Math.min(offset + state.pageSize(), all.size())
            def page = offset >= all.size() ? [] : all.subList(offset, end)
            return new PaginatedGrid.Page<>(page, all.size())
        }
    }

    static PaginatedGrid<Item> newGrid(FakeLoader loader, List<Item> items = []) {
        def grid = new Grid<Item>()
        grid.addColumn({ it.id })
        def paginated = new PaginatedGrid<>(grid, loader, { it.id }, "item", DEFAULT_SORT)
        return paginated
    }

    def "default list state uses page one, default page size and supplied default sort"() {
        given:
        def loader = new FakeLoader([new Item("a")])
        def paginated = newGrid(loader)

        expect:
        paginated.listState().page() == 1
        paginated.listState().pageSize() == ListStateCodec.DEFAULT_PAGE_SIZE
        paginated.listState().filter() == ""
        paginated.listState().sort() == DEFAULT_SORT
    }

    def "setListState loads the current page and reports total to the pager"() {
        given:
        def items = (1..30).collect { new Item("id-$it") }
        def loader = new FakeLoader(items)
        def paginated = newGrid(loader)

        when: "a page of 12 items (page 2) is requested"
        paginated.setListState(new ListState(2, 12, "", DEFAULT_SORT))

        then: "the loader sliced the correct offset/limit window"
        loader.loadCount == 1
        paginated.grid().getGenericDataView().getItems().toList()*.id == (13..24).collect { "id-$it" }
        paginated.selectedIds().isEmpty()
    }

    def "out-of-range page is clamped to the last valid page"() {
        given:
        def items = (1..5).collect { new Item("id-$it") }
        def loader = new FakeLoader(items)
        def paginated = newGrid(loader)

        when: "a page beyond the result set is requested"
        paginated.setListState(new ListState(5, 12, "", DEFAULT_SORT))

        then: "the page is clamped to page 1 (5 items, 12 per page => 1 page)"
        paginated.listState().page() == 1
    }

    def "selection survives a page change (cross-page selection)"() {
        given:
        def items = (1..20).collect { new Item("id-$it") }
        def loader = new FakeLoader(items)
        def paginated = newGrid(loader)
        paginated.setListState(new ListState(1, 10, "", DEFAULT_SORT))

        and: "an item not on the current page is selected via the identifier set"
        paginated.select(Set.of("id-15", "id-16"))
        paginated.selectedIds() == ["id-15", "id-16"] as Set

        when: "a different page is loaded"
        paginated.setListState(new ListState(2, 10, "", DEFAULT_SORT))

        then: "the selection is preserved across the page render"
        paginated.selectedIds() == ["id-15", "id-16"] as Set
    }

    def "clearing the selection reconciles the visible rows and empties the selection"() {
        given:
        def items = (1..10).collect { new Item("id-$it") }
        def loader = new FakeLoader(items)
        def paginated = newGrid(loader)
        paginated.setListState(new ListState(1, 10, "", DEFAULT_SORT))
        paginated.select(Set.of("id-1", "id-2"))

        when:
        paginated.deselect(Set.of("id-1", "id-2"))

        then:
        paginated.selectedIds().isEmpty()
    }

    def "Page record rejects a negative total and null items"() {
        when: "a negative total is used"
        new PaginatedGrid.Page<>([], -1)

        then:
        thrown(IllegalArgumentException)

        when: "null items are used"
        new PaginatedGrid.Page<>(null, 0)

        then:
        thrown(NullPointerException)
    }

    def "empty result set hides the pager and shows an empty state"() {
        given:
        def loader = new FakeLoader([])
        def paginated = newGrid(loader)

        when:
        paginated.setListState(new ListState(1, 12, "", DEFAULT_SORT))

        then:
        !paginated.grid().getGenericDataView().getItems().toList()
    }

    def "adding a filter reloads from the first page"() {
        given:
        def items = (1..25).collect { new Item("id-$it") }
        def loader = new FakeLoader(items)
        def paginated = newGrid(loader)
        paginated.setListState(new ListState(3, 12, "", DEFAULT_SORT))

        when:
        paginated.setListState(paginated.listState().withFilter("x").withPage(1))

        then:
        paginated.listState().page() == 1
        paginated.listState().filter() == "x"
    }

    def "controlled mode does not auto-load on attach and does not render toolbar or pager"() {
        given:
        def items = (1..30).collect { new Item("id-$it") }
        def loader = new FakeLoader(items)
        def grid = new Grid<Item>()
        grid.addColumn({ it.id })
        def paginated = new PaginatedGrid<>(grid, loader, { it.id }, "item", DEFAULT_SORT,
            false, false, false)

        expect: "no page is loaded and no internal controls are rendered"
        loader.loadCount == 0
        !paginated.getChildren()
            .anyMatch { c -> c.class.simpleName == "PaginationBar"
                || c.class.simpleName == "TextField" }

        when: "the owning view drives the state explicitly"
        paginated.setListState(new ListState(2, 10, "", DEFAULT_SORT))

        then: "the page is loaded exactly as in standalone mode"
        loader.loadCount == 1
        paginated.grid().getGenericDataView().getItems().toList()*.id == (11..20).collect { "id-$it" }
    }

    PaginatedGrid<Item> newSortablePaginatedGrid(FakeLoader loader, SortOrder defaultSort) {
        // value provider + sort property only (no explicit comparator), as in the raw data grids;
        // the grid still re-sorts in-memory, so the sort indicator must be synced to the state.
        def grid = new Grid<Item>()
        grid.addColumn({ it.id })
            .setKey("id")
            .setSortProperty("id")
        grid.sort([new GridSortOrder<>(grid.getColumnByKey("id"), SortDirection.ASCENDING)])
        return new PaginatedGrid<>(grid, loader, { it.id }, "item", defaultSort,
            false, false, false)
    }

    def "setListState with a descending sort displays the page descending, not ascending"() {
        given: "a sortable paginated grid whose backend page is already descending"
        def loader = new FakeLoader([new Item("c"), new Item("b"), new Item("a")])
        def paginated = newSortablePaginatedGrid(loader, new SortOrder("id", false))

        when: "a descending sort is applied"
        paginated.setListState(new ListState(1, 10, "", new SortOrder("id", true)))

        then: "the grid matches the applied sort instead of re-sorting by a stale ascending indicator"
        paginated.grid().getGenericDataView().getItems().toList()*.id == ["c", "b", "a"]
    }

    def "setListState with an ascending sort displays the page ascending"() {
        given: "a sortable paginated grid whose backend page is ascending"
        def loader = new FakeLoader([new Item("a"), new Item("b"), new Item("c")])
        def paginated = newSortablePaginatedGrid(loader, new SortOrder("id", false))

        when: "an ascending sort is applied"
        paginated.setListState(new ListState(1, 10, "", new SortOrder("id", false)))

        then:
        paginated.grid().getGenericDataView().getItems().toList()*.id == ["a", "b", "c"]
    }

    def "showSearch is true by default and renders the search field"() {
        given:
        def grid = new Grid<Item>()
        grid.addColumn({ it.id })
        def loader = new FakeLoader([new Item("a")])
        def paginated = new PaginatedGrid<>(grid, loader, { it.id }, "item", DEFAULT_SORT)

        expect:
        searchFieldOf(paginated) != null
    }

    def "showSearch=false keeps the toolbar but omits the search field"() {
        given:
        def grid = new Grid<Item>()
        grid.addColumn({ it.id })
        def loader = new FakeLoader([new Item("a")])
        def paginated = new PaginatedGrid<>(grid, loader, { it.id }, "item", DEFAULT_SORT,
                true, false, true, false)

        expect:
        toolbarOf(paginated) != null
        searchFieldOf(paginated) == null
    }

    private static com.vaadin.flow.component.Component toolbarOf(PaginatedGrid<?> paginated) {
        return paginated.children.toList()
                .find { it.element.classList.contains("paginated-grid-toolbar") }
    }

    private static com.vaadin.flow.component.Component searchFieldOf(PaginatedGrid<?> paginated) {
        def toolbar = toolbarOf(paginated)
        return toolbar == null ? null : toolbar.children.toList()
                .find { it instanceof com.vaadin.flow.component.textfield.TextField }
    }
}
