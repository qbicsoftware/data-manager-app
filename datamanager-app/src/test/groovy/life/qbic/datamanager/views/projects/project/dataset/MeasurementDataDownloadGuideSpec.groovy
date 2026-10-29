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

    def "both download tools are shown, each copyable"() {
        given:
        def guide = new MeasurementDataDownloadGuide()

        when: "the dialog's tabbed command block is inspected"
        // TabSheet keeps its content in a slot rather than among its child components, so the
        // tabs are read from the TabSheet itself.
        def tabs = collectTabSheets(guide)

        then: "one tabbed block, holding both tools"
        tabs.size() == 1
        def sheet = (com.vaadin.flow.component.tabs.TabSheet) tabs[0]
        // TabSheet keeps content in a slot, so it is read through getComponent(Tab), not through
        // getChildren().
        def contents = (0..<2).collect { sheet.getComponent(sheet.getTabAt(it)) }
        contents.size() == 2
        contents.every { it != null && it.getClass().getSimpleName() == "CodeBlock" }
    }

    def "the commands are a shape, not a pastable line"() {
        given:
        def guide = new MeasurementDataDownloadGuide()

        when:
        def labels = collectCommandTexts(guide)

        then: "both commands are offered and each names both placeholders"
        labels.size() == 2
        labels.every { it.contains("<URL>") }
        labels.every { it.contains("<TOKEN>") }

        and: "the tools themselves are named, so a reader can tell the tabs apart"
        def prompt = String.valueOf((char) 36) + " "
        labels.any { it.contains(prompt + "curl") }
        labels.any { it.contains(prompt + "wget") }
    }

    /**
     * The concatenated text of a component and its descendants.
     *
     * <p>Only components implementing {@code HasText} carry text; icons and layout wrappers do not,
     * so they contribute their children's text and nothing of their own.</p>
     */
    private static String textOf(com.vaadin.flow.component.Component component) {
        StringBuilder text = new StringBuilder()
        if (component instanceof com.vaadin.flow.component.HasText) {
            text.append(component.getText())
        }
        for (var child : component.getChildren().toList()) {
            text.append(textOf(child))
        }
        return text.toString()
    }

    /** Every TabSheet rendered inside the given component tree. */
    private static List<com.vaadin.flow.component.Component> collectTabSheets(
            com.vaadin.flow.component.Component root) {
        List<com.vaadin.flow.component.Component> found = []
        for (var child : root.getChildren().toList()) {
            if (child.getClass().getSimpleName() == "TabSheet") {
                found.add(child)
            }
            found.addAll(collectTabSheets(child))
        }
        return found
    }

    /** The rendered command text of every CodeBlock the dialog's tab sheets hold. */
    private static List<String> collectCommandTexts(
            com.vaadin.flow.component.Component root) {
        List<String> found = []
        for (var sheet : collectTabSheets(root)) {
            def tabSheet = (com.vaadin.flow.component.tabs.TabSheet) sheet
            for (int index = 0; index < 2; index++) {
                def content = tabSheet.getComponent(tabSheet.getTabAt(index))
                if (content != null) {
                    // CodeBlock renders the command into a child Span and its own getText() is
                    // empty, so the text is read from the component tree.
                    found.add(textOf(content))
                }
            }
        }
        return found
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
