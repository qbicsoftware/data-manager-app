package life.qbic.datamanager.views.projects.project.dataset

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pins the two export scopes of the "Getting the data" dialog.
 *
 * <p>The rule is the same one the measurement edit dialog follows: a baseline action that always
 * works, plus the narrower convenience scope when there is something to narrow to. The whole-dataset
 * export must therefore never depend on a selection — a reader who has selected nothing is the
 * common case ("I just want my files"), and a dialog whose only action is disabled is a dead end.</p>
 */
class MeasurementDataDownloadGuideSpec extends Specification {

    def "the whole-dataset export is offered when nothing is selected"() {
        given:
        def guide = new MeasurementDataDownloadGuide()

        when:
        guide.setSelectionCount(0)

        then: "the baseline action stays available — this is the common case"
        def actions = actionsOf(guide)
        actions.size() == 2
        actions[0].getText().contains("all measurements")
        actions[0].isVisible()
        actions[0].isEnabled()
    }

    def "the selection-scoped export is hidden when nothing is selected"() {
        given:
        def guide = new MeasurementDataDownloadGuide()

        when:
        guide.setSelectionCount(0)

        then: "there is nothing to scope to, so the narrower action is absent, not disabled"
        def actions = actionsOf(guide)
        !actions[1].isVisible()
    }

    def "both scopes are offered when measurements are selected"() {
        given:
        def guide = new MeasurementDataDownloadGuide()

        when:
        guide.setSelectionCount(7)

        then:
        def actions = actionsOf(guide)
        actions.size() == 2
        actions[0].isVisible()
        actions[1].isVisible()
        actions[1].getText().contains("7 selected")
    }

    @Unroll
    def "the selection-scoped label states how many measurements it covers (#count)"() {
        given:
        def guide = new MeasurementDataDownloadGuide()

        when:
        guide.setSelectionCount(count)

        then: "the reader can tell the two scopes apart without counting checkboxes"
        actionsOf(guide)[1].getText() == expected

        where:
        count || expected
        1     || "Export location of the selected measurement"
        7     || "Export locations of the 7 selected measurements"
        120   || "Export locations of the 120 selected measurements"
    }

    def "the primary action is the one that always works"() {
        given:
        def guide = new MeasurementDataDownloadGuide()

        when:
        guide.setSelectionCount(3)

        then: "the whole-dataset export is primary, the narrower scope is tertiary"
        def actions = actionsOf(guide)
        actions[0].getThemeNames().contains("primary")
        actions[1].getThemeNames().contains("tertiary")
    }

    def "the export carries the scope it was asked for"() {
        given:
        def guide = new MeasurementDataDownloadGuide()
        def requested = []
        guide.addExportRequestedListener { event -> requested << event.scope() }
        def actions = actionsOf(guide)
        guide.setSelectionCount(2)

        when:
        actions[0].click()
        actions[1].click()

        then:
        requested == [MeasurementDataDownloadGuide.ExportScope.ALL,
                      MeasurementDataDownloadGuide.ExportScope.SELECTED]
    }

    def "returning to an empty selection hides the narrower scope again"() {
        given:
        def guide = new MeasurementDataDownloadGuide()
        guide.setSelectionCount(4)
        actionsOf(guide)[1].isVisible()

        when: "the reader clears the selection while the dialog is open"
        guide.setSelectionCount(0)

        then:
        !actionsOf(guide)[1].isVisible()
        actionsOf(guide)[0].isVisible()
    }

    /** The two export buttons, in the order the dialog renders them. */
    private static List<com.vaadin.flow.component.button.Button> actionsOf(
            MeasurementDataDownloadGuide guide) {
        // Walked explicitly with casts: getChildren() yields a Stream, a plain loop keeps the
        // traversal readable, and pattern-matching bindings are not usable in this static method.
        List<com.vaadin.flow.component.button.Button> buttons = []
        for (var child : guide.getChildren().toList()) {
            if (!(child instanceof com.vaadin.flow.component.html.Div)) {
                continue
            }
            def step = (com.vaadin.flow.component.html.Div) child
            for (var stepChild : step.getChildren().toList()) {
                if (!(stepChild instanceof com.vaadin.flow.component.html.Div)) {
                    continue
                }
                def inner = (com.vaadin.flow.component.html.Div) stepChild
                if (!inner.getClassNames().contains(
                        "measurement-dataset-download-guide-actions")) {
                    continue
                }
                for (var row : inner.getChildren().toList()) {
                    def rowDiv = (com.vaadin.flow.component.html.Div) row
                    for (var cell : rowDiv.getChildren().toList()) {
                        if (cell instanceof com.vaadin.flow.component.button.Button) {
                            buttons.add((com.vaadin.flow.component.button.Button) cell)
                        }
                    }
                }
            }
        }
        return buttons
    }
}
