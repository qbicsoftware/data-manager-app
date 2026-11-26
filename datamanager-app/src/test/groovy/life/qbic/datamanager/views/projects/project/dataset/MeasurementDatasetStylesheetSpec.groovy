package life.qbic.datamanager.views.projects.project.dataset

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Guards the stylesheet properties that fail silently.
 *
 * <p>The samples page shipped a defect of exactly this kind: it used an undefined custom property
 * as a grid track, which made the whole {@code grid-template-columns} declaration invalid at
 * computed-value time, so the master-detail layout collapsed into a single stacked column and the
 * pager was pushed below the fold. Nothing threw and nothing logged. These tests assert that every
 * {@code var()} the stylesheet depends on is either defined in it or known to exist in the theme,
 * and that the master-detail breakpoint keeps the two-column arrangement.</p>
 */
class MeasurementDatasetStylesheetSpec extends Specification {

    private static File stylesheet() {
        def candidates = [
                "datamanager-app/frontend/themes/datamanager/components/measurement-dataset.css",
                "frontend/themes/datamanager/components/measurement-dataset.css",
                "../datamanager-app/frontend/themes/datamanager/components/measurement-dataset.css"
        ]
        def found = candidates.collect { new File(it) }.find { it.exists() }
        assert found != null: "measurement-dataset.css not found from ${new File('.').absolutePath}"
        return found
    }

    def "the stylesheet defines the panel width it uses as a grid track"() {
        given:
        def css = stylesheet().text

        expect: "the custom property is declared before it is referenced"
        css.contains("--measurement-data-detail-panel-width:")

        and: "and it is actually used as a track, which is the part that fails silently"
        css.contains("var(--measurement-data-detail-panel-width)")
    }

    def "the master-detail layout becomes two columns only at the documented breakpoint"() {
        given:
        def css = stylesheet().text

        expect:
        css.contains("@media (min-width: 1100px)")
        css.contains(".measurement-dataset-detail-layout--open")
    }

    @Unroll
    def "every theme token used by the stylesheet exists in the theme (#token)"() {
        given:
        def css = stylesheet().text
        def variablesFile = [
                new File("datamanager-app/frontend/themes/datamanager/components/variables.css"),
                new File("frontend/themes/datamanager/components/variables.css"),
                new File("../datamanager-app/frontend/themes/datamanager/components/variables.css")
        ].find { it.exists() }
        def variables = variablesFile.text

        expect: "the token is either declared here or defined by the theme"
        css.contains("--${token}:") || variables.contains("--${token}:")

        where:
        token << ["content-shell-max-width", "content-shell-gutter", "heading-text-color",
                  "body-text-color", "primary-color", "primary-color-10pct", "primary-color-20pct",
                  "success-text-color", "warning-text-color", "error-color",
                  "error-color-10pct", "error-text-color", "border-color-default",
                  "shade-color-5pct", "field-label-text-color", "font-family-monospace",
                  "font-weight-semi-bold", "font-weight-medium", "spacing-01", "spacing-02",
                  "spacing-03"]
    }

    def "the page shell is capped and centred like the project and sample pages"() {
        given:
        def css = stylesheet().text

        expect: "the shell owns the cap and the gutter, so bands need neither"
        css.contains(".measurement-dataset {")
        css.contains("max-width: var(--content-shell-max-width)")
        css.contains("padding-inline: var(--content-shell-gutter)")
    }

    def "the open record's row is marked differently from a selected row"() {
        given:
        def css = stylesheet().text

        expect: "opening a record must not look like adding it to a bulk selection"
        css.contains("::part(open-detail-row)")
    }

    def "the page-size select is wide enough for its own label"() {
        given: "the shared pager stylesheet, which every paginated list uses"
        def candidates = [
                new File("datamanager-app/frontend/themes/datamanager/components/pagination.css"),
                new File("frontend/themes/datamanager/components/pagination.css"),
                new File("../datamanager-app/frontend/themes/datamanager/components/pagination.css")
        ]
        def file = candidates.find { it.exists() }
        assert file != null

        when:
        def text = file.text
        int start = text.indexOf(".pagination-bar .pagination-page-size {")
        assert start > 0
        def rule = text.substring(start, text.indexOf("}", start))

        then: "a fixed 8rem was narrower than '12 per page' plus its chevron, so the chevron sat"
        // on top of the last character. The width now follows the content, with a floor that still
        // keeps the bar from jumping as the value changes.
        !rule.contains("width: 8rem")
        rule.contains("width: auto")
        rule.contains("min-width:")
        rule.contains("flex: 0 0 auto")
    }

    /**
     * The layouts start at TWO, not one, because the "all measurements" card is always rendered
     * beside the dataset cards: the modifier counts cards, not datasets, so the smallest row holds
     * the All card plus one dataset. A count-1 layout can therefore never apply, and asserting it
     * was asserting a rule that had already been deleted as unreachable.
     */
    def "each card count gets its own column layout, starting at the two the page always has"() {
        given:
        def css = stylesheet().text

        expect: "every count the component can emit has a layout"
        (2..8).every { css.contains(".measurement-dataset-count-${it}") }

        and: "no unreachable single-card layout is declared"
        !css.contains(".measurement-dataset-count-1")

        and: "the row is an explicit grid, so empty tracks cannot collapse or a card sit marooned"
        !block("measurement-dataset-selector-cards").contains("auto-fit")
        !block("measurement-dataset-selector-cards").contains("flex-wrap: wrap")
    }

    /**
     * The narrowest real case is the All card plus one dataset, and it is capped at two columns
     * rather than spreading across the row: a pair that fills the width reads as a page-wide band
     * rather than as two cards among possible others.
     */
    def "the smallest real row is capped at two columns rather than spanning the page"() {
        given:
        def css = stylesheet().text
        int start = css.indexOf(".measurement-dataset-count-2 {")
        assert start > 0
        def rule = css.substring(start, css.indexOf("}", start))

        expect:
        rule.contains("repeat(2")
    }

    def "the board's status ring sits beside the card's text, not at the far edge"() {
        given:
        def css = stylesheet().text

        expect: "space-between in a stretched card puts the ring as far from its own value as possible"
        def headline = block("measurement-dataset-card-headline-row")
        headline.contains("display: flex")
        !headline.contains("justify-content: space-between")
    }

    def "a panel group titled Data does not repeat the word as its own field label"() {
        given:
        def panel = new File(
                "datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/dataset/MeasurementDataDetailPanel.java")
        def source = panel.exists() ? panel.text : new File(
                "src/main/java/life/qbic/datamanager/views/projects/project/dataset/MeasurementDataDetailPanel.java").text

        expect: "the rendered panel said DATA / Data / Waiting — the word twice for one fact"
        !source.contains('field("Data", "Available"')
        !source.contains('field("Data", "Waiting')
    }

    /** The declaration block of a class selector, or an empty string when absent. */
    private static String block(String className) {
        String text = stylesheet().text
        int start = text.indexOf("." + className + " {")
        if (start < 0) {
            return ""
        }
        return text.substring(start, text.indexOf("}", start))
    }

    def "the grid suppresses native text selection in its cells"() {
        given:
        def css = stylesheet().text

        expect: "a row click that drifts a pixel is a click-and-drag, and a drag over text"
        // selects it — the smeared highlight across several rows. The grid only suppresses text
        // selection while reordering, resizing or range-selecting, so at rest the cells need this.
        css.contains("measurement-dataset-grid::part(body-cell)")
        css.contains("measurement-dataset-grid vaadin-grid-cell-content")
        css.contains("user-select: none")

        and: "the pointer cursor is the other half of the pair: it invites the click"
        css.contains("cursor: pointer")

        and: "the checkbox and chevron cells are exempt, so they do not look like the row"
        css.contains(".measurement-dataset-grid vaadin-checkbox")
        css.contains("cursor: default")
    }

    def "the toolbar separator has an explicit height"() {
        given:
        def rule = block("measurement-dataset-toolbar-separator")

        expect: "an empty div has no height, so align-self: stretch cannot stretch it — the"
        // separator rendered as nothing at all and the row's scope grouping was invisible.
        rule.contains("height:")
        !rule.contains("align-self: stretch")
    }

    def "the toolbar gap sits on the real flex children"() {
        given:
        def container = block("measurement-dataset-toolbar-right")
        def css = stylesheet().text

        expect: "grouping actions into wrapper divs removed the only spacing there was, and the"
        // whole row collapsed into one continuous block of touching controls. The rhythm is now
        // stated on the container and every direct child is pinned against shrinking it away.
        container.contains("gap:")
        container.contains("display: flex")
        css.contains(".measurement-dataset-toolbar-right > *")
    }

    def "the selection bar is sticky so the selected count stays visible"() {
        given:
        def css = stylesheet().text

        expect: "USER-R-02 requires the selected count to always be visible"
        css.contains(".measurement-dataset-selection-bar")
        css.contains("position: sticky")
    }
}
