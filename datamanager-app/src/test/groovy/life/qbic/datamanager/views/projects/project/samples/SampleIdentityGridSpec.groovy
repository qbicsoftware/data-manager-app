package life.qbic.datamanager.views.projects.project.samples

import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.general.pagination.ListState
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.projectmanagement.application.api.AsyncProjectService
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.ProjectId
import org.springframework.context.support.StaticMessageSource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Specification

/**
 * Covers the identity-first grid: the default view carries the columns a scan needs, and the
 * columns retired from it stay reachable through the Show/Hide Columns menu rather than being lost.
 *
 * The page previously rendered twelve flat columns, most of which were constant per experiment, so
 * the user had to decide per row which columns mattered. Keeping the retired columns available (but
 * hidden) preserves the power-user path without paying the scanning cost by default.
 */
class SampleIdentityGridSpec extends Specification {

    private SampleDetailsComponent newComponent() {
        def messageSource = new StaticMessageSource()
        def messages = new MessageSourceNotificationFactory(messageSource)
        def service = Stub(AsyncProjectService) {
            countSamples(_, _, _) >> Mono.just(0)
            getSamplePreviews(_, _, _, _, _) >> Flux.empty()
        }
        def context = new Context()
            .with(ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5"))
            .with(ExperimentId.parse("3270ce7f-4092-40e3-9c4c-ce7adb688bf8"))
            .withProjectCode("QABCDE")
        return new SampleDetailsComponent(service, messages, context)
    }

    private static List<String> visibleHeaders(SampleDetailsComponent component) {
        component.paginatedGrid().grid().getColumns().stream()
            .filter { it.isVisible() }
            .map { it.getHeaderText() }
            .filter { it != null && !it.isBlank() }
            .toList()
    }

    private static List<String> allHeaders(SampleDetailsComponent component) {
        component.paginatedGrid().grid().getColumns().stream()
            .map { it.getHeaderText() }
            .filter { it != null && !it.isBlank() }
            .toList()
    }

    def "the default grid carries identity columns only"() {
        given: "a sample page component"
        def component = newComponent()

        expect: "the identity columns are visible by default"
        def visible = visibleHeaders(component)
        visible.contains("Sample ID")
        visible.contains("Sample name")
        visible.contains("Condition")
        visible.contains("Batch")
    }

    def "columns constant per experiment are not in the default view"() {
        given: "a sample page component"

        when: "the visible columns are inspected"
        def visible = visibleHeaders(newComponent())

        then: "the per-experiment constants and audit values are absent by default"
        !visible.contains("Species")
        !visible.contains("Specimen")
        !visible.contains("Analyte")
        !visible.contains("Analysis to Perform")
        !visible.contains("Registration time")
        !visible.contains("Modification time")
        !visible.contains("Biological Replicate")
        !visible.contains("Comment")
    }

    def "the retired columns remain available so the flat table can be restored"() {
        given: "a sample page component"

        when: "every configured column is inspected, visible or not"
        def all = allHeaders(newComponent())

        then: "the retired columns are still part of the grid"
        all.contains("Species")
        all.contains("Specimen")
        all.contains("Analyte")
        all.contains("Analysis to Perform")
        all.contains("Registration time")
        all.contains("Modification time")
        all.contains("Biological Replicate")
        all.contains("Comment")
    }

    def "the sample identifier column stays frozen so it survives horizontal scrolling"() {
        given: "a sample page component"

        when: "the identifier column is located"
        def idColumn = newComponent().paginatedGrid().grid().getColumns().stream()
            .filter { it.getHeaderText() == "Sample ID" }
            .findFirst()

        then: "it is present and frozen"
        idColumn.isPresent()
        idColumn.get().isFrozen()
    }
}
