package life.qbic.datamanager.views.projects.project.samples

import com.vaadin.flow.component.grid.ItemClickEvent
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.projectmanagement.application.api.AsyncProjectService
import life.qbic.projectmanagement.application.sample.SamplePreview
import life.qbic.projectmanagement.domain.model.OntologyTerm
import life.qbic.projectmanagement.application.ontology.OntologyClass
import life.qbic.projectmanagement.domain.model.experiment.Condition
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel
import life.qbic.projectmanagement.domain.model.experiment.VariableName
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod
import life.qbic.projectmanagement.domain.model.sample.SampleCode
import life.qbic.projectmanagement.domain.model.sample.SampleId
import org.springframework.context.support.StaticMessageSource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Specification

/**
 * Covers how a record is opened from the grid.
 *
 * <p>The row itself is the primary target: the chevron sits at the far right of a wide row, so
 * making it the only way in forces the user to aim at a small icon. Clicking anywhere on the row
 * opens the same record.</p>
 *
 * <p>Opening a record must stay independent of the grid's bulk selection. Vaadin only selects on a
 * row click when the selection column's {@code autoSelect} is enabled, and this grid does not
 * enable it -- so the two concerns never collide and a bulk Delete cannot reach a sample the user
 * merely inspected.</p>
 */
class SampleRowActivationSpec extends Specification {

    private static final String SAMPLE_ID = "4270ce7f-4092-40e3-9c4c-ce7adb688bf9"

    private static final String SAMPLE_CODE = "Q256A6001AO"

    private SampleDetailsComponent newComponent() {
        def messageSource = new StaticMessageSource()
        def messages = new MessageSourceNotificationFactory(messageSource)
        def service = Stub(AsyncProjectService) {
            countSamples(_, _, _) >> Mono.just(1)
            getSamplePreviews(_, _, _, _, _) >> Flux.just(samplePreview())
        }
        def context = new Context()
            .with(ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5"))
            .with(ExperimentId.parse("3270ce7f-4092-40e3-9c4c-ce7adb688bf8"))
            .withProjectCode("QABCDE")
        return new SampleDetailsComponent(service, messages, context)
    }

    private static SamplePreview samplePreview() {
        def levels = [VariableLevel.create(VariableName.create("Time"),
            ExperimentalValue.create("100", "h"))]
        def group = ExperimentalGroup.create("Group 1", Condition.create(levels), 1)
        return SamplePreview.create(
            ExperimentId.parse("3270ce7f-4092-40e3-9c4c-ce7adb688bf8"),
            SampleId.parse(SAMPLE_ID),
            SampleCode.create(SAMPLE_CODE).code(),
            "Batch 1",
            "Sample 1",
            "1",
            group,
            term("Homo sapiens", "NCBITaxon:9606"),
            term("Tumour organoid", "EFO:0001085"),
            term("Peptide", "CHEBI:15841"),
            AnalysisMethod.SIXTEEN_S,
            null)
    }

    private static OntologyTerm term(String label, String curie) {
        def ontologyClass = new OntologyClass("NCBITaxon", "1.0", "https://example.org/" + curie,
            label, curie, label, "https://example.org/class/" + curie)
        return OntologyTerm.from(ontologyClass)
    }

    private static ItemClickEvent itemClick(SampleDetailsComponent component, SamplePreview item) {
        def grid = component.paginatedGrid().grid()
        // ItemClickEvent resolves the item through the grid's key mapper, so the event has to be
        // built with the item's key (as the client would send it) rather than the item itself.
        String itemKey = grid.getDataCommunicator().getKeyMapper().key(item)
        return new ItemClickEvent<SamplePreview>(grid, true, itemKey, "sampleName",
            0, 0, 0, 0, 0, 0, false, false, false, false)
    }

    def "clicking a row opens that record in the detail panel"() {
        given: "a grid with one sample, and the panel closed"
        def component = newComponent()
        def panel = detailPanelOf(component)
        assert !panel.isVisible()

        when: "the user clicks anywhere on the row"
        component.paginatedGrid().grid().fireEvent(itemClick(component, samplePreview()))

        then: "the record is rendered in the panel"
        panel.isVisible()
        panel.element.getTextRecursively().contains("Sample 1")
    }

    def "opening a record does not add it to the bulk selection"() {
        given: "a grid with one sample"
        def component = newComponent()

        when: "a row is clicked to read the record"
        component.paginatedGrid().grid().fireEvent(itemClick(component, samplePreview()))

        then: "the panel is open but nothing is selected, so a bulk Delete cannot reach it"
        detailPanelOf(component).isVisible()
        component.paginatedGrid().selectedIds().isEmpty()
    }

    def "the open row is marked so the panel can be traced back to it"() {
        given: "a grid with one sample"
        def component = newComponent()
        def grid = component.paginatedGrid().grid()

        when: "a row is clicked"
        grid.fireEvent(itemClick(component, samplePreview()))

        then: "that row carries the marker part the stylesheet tints"
        grid.getPartNameGenerator().apply(samplePreview()) == "open-detail-row"
    }

    def "closing the record clears the row marker"() {
        given: "a grid with an open record"
        def component = newComponent()
        def grid = component.paginatedGrid().grid()
        grid.fireEvent(itemClick(component, samplePreview()))
        assert grid.getPartNameGenerator().apply(samplePreview()) == "open-detail-row"

        when: "the user dismisses the panel"
        // Fired exactly as the panel's own close button does, so the owner's close handling (which
        // clears the marker and restores focus) runs as it would in the UI.
        def panel = detailPanelOf(component)
        panel.fireEvent(new SampleDetailPanel.Closed(panel, true))

        then: "the marker is gone, so no row stays tinted"
        grid.getPartNameGenerator().apply(samplePreview()) == null
    }

    private static SampleDetailPanel detailPanelOf(SampleDetailsComponent component) {
        // The panel is the only SampleDetailPanel in the component's subtree.
        return (SampleDetailPanel) component.getChildren()
            .flatMap { it.getChildren() }
            .filter { it instanceof SampleDetailPanel }
            .findFirst()
            .orElseThrow()
    }
}
