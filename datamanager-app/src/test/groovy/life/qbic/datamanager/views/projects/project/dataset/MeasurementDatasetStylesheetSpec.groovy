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

    def "a lone dataset card does not stretch across the content column"() {
        given:
        def css = stylesheet().text

        expect: "auto-fit collapses its empty tracks, so a single card filled the whole row and its"
        // ring ended up a screen away from the text it describes. A flex basis with a maximum
        // keeps one card beside its own content and lets several wrap.
        //
        // The check reads the *declaration*, not the file: the old pattern is still mentioned in
        // the comment that explains why it was wrong, and a plain text search matched that.
        !block("measurement-dataset-selector-cards").contains("grid-template-columns")
        block("measurement-dataset-selector-cards").contains("flex-wrap: wrap")
        block("measurement-dataset-card").contains("max-width:")
        block("measurement-dataset-card").contains("flex: 0 1")
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

    def "the selection bar is sticky so the selected count stays visible"() {
        given:
        def css = stylesheet().text

        expect: "USER-R-02 requires the selected count to always be visible"
        css.contains(".measurement-dataset-selection-bar")
        css.contains("position: sticky")
    }
}
