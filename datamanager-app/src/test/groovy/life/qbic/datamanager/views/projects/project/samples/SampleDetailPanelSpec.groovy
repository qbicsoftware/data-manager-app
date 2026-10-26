package life.qbic.datamanager.views.projects.project.samples

import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.domain.model.experiment.Condition
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel
import life.qbic.projectmanagement.domain.model.experiment.VariableName
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import life.qbic.projectmanagement.domain.model.sample.SampleId
import life.qbic.projectmanagement.application.sample.SamplePreview
import spock.lang.Specification

/**
 * Covers the identity-first sample detail panel: the panel is the only surface that renders a
 * sample's full record, so it must carry every field the grid omits, must render a missing value
 * explicitly, and must not begin any mutation.
 */
class SampleDetailPanelSpec extends Specification {

    private static SamplePreview preview(String name = "Sample 1",
                                         String batch = "Batch 1",
                                         String replicate = "1",
                                         String comment = null,
                                         OntologyTerm species = term("Homo sapiens", "NCBITaxon:9606")) {        def levels = [VariableLevel.create(VariableName.create("Time"),
            ExperimentalValue.create("100", "h"))]
        def group = ExperimentalGroup.create("Group 1", Condition.create(levels), 1)
        return SamplePreview.create(
            ExperimentId.parse("3270ce7f-4092-40e3-9c4c-ce7adb688bf8"),
            SampleId.parse("4270ce7f-4092-40e3-9c4c-ce7adb688bf9"),
            SampleCode.create("Q256A6001AO").code(),
            batch,
            name,
            replicate,
            group,
            species,
            term("Tumour organoid", "EFO:0001085"),
            term("Peptide", "CHEBI:15841"),
            AnalysisMethod.SIXTEEN_S,
            comment)
    }

    private static OntologyTerm term(String label, String curie) {
        def ontologyClass = new OntologyClass("NCBITaxon", "1.0", "https://example.org/" + curie,
            label, curie, label, "https://example.org/class/" + curie)
        return OntologyTerm.from(ontologyClass)
    }

    def "renders every field the grid omits"() {
        given: "a panel and a sample with a full record"
        def panel = new SampleDetailPanel()
        def sample = preview("Sample 42", "Batch 7", "3", "ok")
        when: "the sample is shown"
        panel.show(sample, { Instant -> "2026-01-22 16:17" })
        and: "the rendered text is collected"
        def text = panel.element.getTextRecursively()

        then: "identity, laboratory, origin and notes are all present"
        text.contains("Sample 42")
        text.contains("Q256A6001AO")
        text.contains("Batch 7")
        text.contains("3")
        text.contains("Homo sapiens")
        text.contains("Tumour organoid")
        text.contains("Peptide")
        text.contains("ok")
        and: "the audit timestamps that left the grid are readable here"
        text.contains("2026-01-22 16:17")
    }

    def "an experiment variable becomes its own condition row"() {
        given: "a panel showing a sample whose condition has one variable"
        def panel = new SampleDetailPanel()

        when: "the sample is shown"
        panel.show(preview(), { Instant -> "-" })

        then: "the variable is named and its value shown, not collapsed into one line"
        def text = panel.element.getTextRecursively()
        text.contains("Time")
        text.contains("100 h")
    }

    def "a missing value renders explicitly rather than as a blank"() {
        given: "a sample without a comment"
        def panel = new SampleDetailPanel()

        when: "the sample is shown"
        panel.show(preview("Sample 1", "Batch 1", "1", null), { Instant -> "-" })

        then: "the empty field is stated with an em dash"
        panel.element.getTextRecursively().contains("\u2014")
    }

    def "closing the panel drops the rendered record"() {
        given: "a panel showing a sample"
        def panel = new SampleDetailPanel()
        panel.show(preview("Sample 42"), { Instant -> "-" })

        when: "the panel is closed"
        panel.close()

        then: "the record is gone, so no stale sample stays in the DOM"
        !panel.isVisible()
        !panel.element.getTextRecursively().contains("Sample 42")
    }
}
