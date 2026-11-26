package life.qbic.datamanager.views.projects.project.samples

import com.vaadin.flow.component.Component
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.projectmanagement.application.api.AsyncProjectService
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.ProjectId
import org.springframework.context.support.StaticMessageSource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Specification

/**
 * Separates two states that look alike on screen but mean opposite things: "this search matched
 * nothing" and "the list could not be loaded".
 *
 * <p>{@code countSamples} resolves to {@code Mono.empty()} whenever the count is zero -- that is the
 * documented signal for "no samples", not a failure. Reading an absent count as an error reported
 * every empty search result as a backend fault, so a legitimate search outcome was answered with a
 * red "Samples could not be loaded." banner and a retry that could only fail again.</p>
 *
 * <p>The error state still has to work for real faults, so both directions are pinned here: an empty
 * result must not raise it, and a rejection must still raise it.</p>
 */
class SampleLoadFailureSpec extends Specification {

    private static final String PROJECT_ID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"

    private static final String EXPERIMENT_ID = "3270ce7f-4092-40e3-9c4c-ce7adb688bf8"

    private SampleDetailsComponent newComponent(Mono<Integer> count, Flux<?> previews) {
        def messageSource = new StaticMessageSource()
        def messages = new MessageSourceNotificationFactory(messageSource)
        def service = Stub(AsyncProjectService) {
            countSamples(_, _, _) >> count
            getSamplePreviews(_, _, _, _, _) >> previews
        }
        def context = new Context()
            .with(ProjectId.parse(PROJECT_ID))
            .with(ExperimentId.parse(EXPERIMENT_ID))
            .withProjectCode("QABCDE")
        return new SampleDetailsComponent(service, messages, context)
    }

    private static void applyFilter(SampleDetailsComponent component, String filter) {
        def grid = component.paginatedGrid()
        grid.setListState(grid.listState().withFilter(filter).withPage(1))
    }

    private static Component withClass(SampleDetailsComponent component, String className) {
        return descendantsOf(component).find { it.getClassNames().contains(className) }
    }

    private static List<Component> descendantsOf(Component root) {
        def found = []
        root.getChildren().forEach { child ->
            found.add(child)
            found.addAll(descendantsOf(child))
        }
        return found
    }

    def "a search that matches nothing is an empty result, not a load failure"() {
        given: "a backend that answers an empty count, exactly as the API does for zero matches"
        def component = newComponent(Mono.empty(), Flux.empty())

        when: "a search term matches nothing"
        applyFilter(component, "NOPE")

        then: "no error is reported, because nothing failed"
        !withClass(component, "sample-error-state").isVisible()

        and: "the grid's empty state carries the message instead"
        withClass(component, "paginated-grid-empty-state").isVisible()
    }

    def "an experiment holding no samples at all loads as empty rather than as failure"() {
        given: "a backend with no samples, which answers an empty count"
        def component = newComponent(Mono.empty(), Flux.empty())

        when: "the first page is loaded with no filter"
        component.paginatedGrid().refresh()

        then: "the page is empty, not broken"
        !withClass(component, "sample-error-state").isVisible()
    }

    def "a genuine backend failure is still surfaced as an error"() {
        given: "a backend whose count call rejects"
        def component = newComponent(Mono.error(new IllegalStateException("backend unavailable")),
            Flux.empty())

        when: "a page is loaded"
        component.paginatedGrid().refresh()

        then: "the failure is shown, so it cannot be mistaken for an empty list"
        // This is the behaviour the error banner exists for; the fix above must not have traded it
        // away for the empty case.
        withClass(component, "sample-error-state").isVisible()
    }

    def "a failure while fetching the page is surfaced as an error"() {
        given: "a backend that counts fine but fails to return the rows"
        def component = newComponent(Mono.just(5), Flux.error(new IllegalStateException("rows gone")))

        when: "a page is loaded"
        component.paginatedGrid().refresh()

        then: "the failure is shown rather than rendered as an empty page"
        withClass(component, "sample-error-state").isVisible()
    }
}
