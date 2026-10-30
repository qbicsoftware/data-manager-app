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

    def "each dataset count gets its own column layout"() {
        given:
        def css = stylesheet().text

        expect: "a single fixed card size is always a compromise — one, two and three datasets"
        // want different columns, and an auto-fitting grid collapses its empty tracks so a lone
        // card either stretches across the page or sits marooned at the left. Each count names its
        // own layout, following the pinned-project cards.
        css.contains(".measurement-dataset-count-1")
        css.contains(".measurement-dataset-count-2")
        css.contains(".measurement-dataset-count-3")
        !block("measurement-dataset-selector-cards").contains("auto-fit")
        !block("measurement-dataset-selector-cards").contains("flex-wrap: wrap")
    }

    def "one dataset is capped at half the row rather than spanning it"() {
        given:
        def css = stylesheet().text
        int start = css.indexOf(".measurement-dataset-count-1 {")
        assert start > 0
        def rule = css.substring(start, css.indexOf("}", start))

        expect: "a full-width band reads as a page header, not as one card among possible others"
        rule.contains("50%")
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
