package life.qbic.datamanager.views.projects.project.measurements.pagination

import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.data.selection.MultiSelectionEvent
import com.vaadin.flow.data.selection.SelectionListener
import life.qbic.datamanager.views.general.pagination.Selection
import life.qbic.datamanager.views.projects.project.measurements.MeasurementDetailsComponent
import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup
import spock.lang.Specification

/**
 * Pins the end-to-end selection semantics of the paginated measurement lists
 * (ADR-0009, USER-R-02): the identifier-based {@link Selection} must
 * survive page renders, tab switches, and the synthetic deselection events that
 * Vaadin fires when a new page is written to a grid.
 *
 * <p>This spec reproduces a real regression: grid selection listeners reacted to
 * server-side {@code setItems} deselection events as if they were user actions,
 * purging every selected measurement that is not on the newly rendered page —
 * the "weird number" in the selection bar after tab switches.</p>
 */
class SelectionIntegrationSpec extends Specification {

    /**
     * A {@link MultiSelectionEvent} stand-in that exposes the added/removed sets
     * a real Grid fires for a selection transition, plus an {@code isFromClient}
     * flag to distinguish user toggles from synthetic server-side deselection
     * after {@code setItems}.
     */
    static class SelectionTransition<T> extends MultiSelectionEvent<Grid<T>, T> {

        private final Set<T> added
        private final Set<T> removed
        private final boolean fromClient

        SelectionTransition(Grid<T> grid, Set<T> oldSelection, Set<T> newSelection,
            boolean fromClient = true) {
            super(grid, grid.asMultiSelect(), oldSelection, fromClient)
            this.fromClient = fromClient
            this.added = new LinkedHashSet<>(newSelection)
            this.added.removeAll(oldSelection)
            this.removed = new LinkedHashSet<>(oldSelection)
            this.removed.removeAll(newSelection)
        }

        @Override
        Set<T> getAddedSelection() {
            return added
        }

        @Override
        Set<T> getRemovedSelection() {
            return removed
        }

        @Override
        boolean isFromClient() {
            return fromClient
        }
    }

    /**
     * A tiny recorder that registers the <em>production</em> reconciliation
     * listener ({@code MeasurementDetailsComponent.createSelectionReconciliationListener})
     * on a grid and lets the spec dispatch a {@link SelectionTransition} to it,
     * mimicking how Vaadin's selection model fires events to registered listeners.
     */
    static class ListenerBackedSelection {

        private final SelectionListener<Grid<Object>, Object> listener
        final Selection selection

        ListenerBackedSelection(Grid<Object> grid, MeasurementDomain domain) {
            this.selection = new Selection(null as Runnable)
            this.listener = MeasurementDetailsComponent
                .createSelectionReconciliationListener(grid, selection, domain)
            grid.addSelectionListener(listener)
        }

        void fire(SelectionTransition<Object> transition) {
            listener.selectionChange(transition)
        }
    }

    def "select all N then rendering a page keeps the full cross-page selection"() {
        given: "an IP grid in multi-select mode with the production reconciliation listener"
        def grid = new Grid<Object>()
        grid.setSelectionMode(Grid.SelectionMode.MULTI)
        def backed = new ListenerBackedSelection(grid, MeasurementDomain.IP)
        def selection = backed.selection
        List<Object> allInfos = (1..40).collect { ipMeasurement(it) }
        selection.select(allInfos.collect { it.measurementId() } as Set<String>)

        when: "the first page (24 rows) is rendered, which makes Vaadin deselect everything"
        def page1 = allInfos.subList(0, 24) as List<Object>
        backed.fire(new SelectionTransition<>(grid, new LinkedHashSet<>(page1), Set.of(), false))

        then: "the selection keeps all 40 ids (no truncation to the page size)"
        selection.count() == 40
        selection.selectedIds() == allInfos.collect { it.measurementId() } as Set
    }

    def "selecting one NGS measurement after a tab switch does not wipe the IP selection"() {
        given: "IP selection carries 40 ids, NGS grid is fresh"
        def ipSelection = new Selection(null as Runnable)
        ipSelection.select((1..40).collect { "IP-MEAS-$it" } as Set<String>)
        def ngsGrid = new Grid<Object>()
        ngsGrid.setSelectionMode(Grid.SelectionMode.MULTI)
        def backed = new ListenerBackedSelection(ngsGrid, MeasurementDomain.NGS)
        def ngsSelection = backed.selection
        def ngsInfo = ngsMeasurement("NGS-MEAS-1")

        when: "the NGS page is rendered after the tab switch and the user selects one row"
        backed.fire(new SelectionTransition<>(ngsGrid, Set.of(), Set.of(ngsInfo)))
        // a second page render for NGS deselects everything first (server-side, ignored)
        backed.fire(new SelectionTransition<>(ngsGrid, Set.of(ngsInfo), Set.of(), false))

        then: "the NGS selection keeps the user's single selection"
        ngsSelection.count() == 1
        ngsSelection.contains("NGS-MEAS-1")

        and: "the IP selection is untouched"
        ipSelection.count() == 40
    }

    def "rendering a new page of IP after tab return does not purge off-page selections"() {
        given: "IP selection holds 40 ids"
        def grid = new Grid<Object>()
        grid.setSelectionMode(Grid.SelectionMode.MULTI)
        def backed = new ListenerBackedSelection(grid, MeasurementDomain.IP)
        def selection = backed.selection
        List<Object> allInfos = (1..40).collect { ipMeasurement(it) }
        selection.select(allInfos.collect { it.measurementId() } as Set<String>)

        when: "a page is rendered (as happens on every tab return / pagination)"
        backed.fire(new SelectionTransition<>(grid,
            new LinkedHashSet<>(allInfos.subList(0, 24)), Set.of(), false))

        then: "all 40 selections survive"
        selection.count() == 40
    }

    private static IpMeasurementLookup.MeasurementInfo ipMeasurement(int i) {
        return new IpMeasurementLookup.MeasurementInfo(
            "IP-MEAS-$i", "P", "E", "IP-MEAS-$i", "name", "facility", null, null,
            null, java.time.Instant.EPOCH, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null,
            List.of())
    }

    private static NgsMeasurementLookup.MeasurementInfo ngsMeasurement(String id) {
        return new NgsMeasurementLookup.MeasurementInfo(
            id, "P", "E", id, "name", "facility", null, null,
            null, java.time.Instant.EPOCH, null, null, null, null, List.of())
    }
}
