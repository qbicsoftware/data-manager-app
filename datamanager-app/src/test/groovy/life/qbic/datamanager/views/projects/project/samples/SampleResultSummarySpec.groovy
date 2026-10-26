package life.qbic.datamanager.views.projects.project.samples

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Span
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
 * Covers the feedback for an active search.
 *
 * <p>Typing in the search field used to produce no visible answer to "how many samples match?" where
 * the user was looking. The page carries two other counts and neither serves that purpose: the
 * header states the experiment's registration total, which must not move while searching, and the
 * pager that carries the filtered total sits below the fold once the list is full.</p>
 *
 * <p>Scope note: the summary is deliberately hidden while no search is active. With no filter the
 * number of matches equals the number of registered samples, which the header already states, so
 * showing it here as well would print the same fact twice.</p>
 */
class SampleResultSummarySpec extends Specification {

    private static final String PROJECT_ID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"

    private static final String EXPERIMENT_ID = "3270ce7f-4092-40e3-9c4c-ce7adb688bf8"

    private SampleDetailsComponent newComponent(int matching) {
        def messageSource = new StaticMessageSource()
        def messages = new MessageSourceNotificationFactory(messageSource)
        def service = Stub(AsyncProjectService) {
            countSamples(_, _, _) >> Mono.just(matching)
            getSamplePreviews(_, _, _, _, _) >> Flux.empty()
        }
        def context = new Context()
            .with(ProjectId.parse(PROJECT_ID))
            .with(ExperimentId.parse(EXPERIMENT_ID))
            .withProjectCode("QABCDE")
        return new SampleDetailsComponent(service, messages, context)
    }

    private static void applyFilter(SampleDetailsComponent component, String filter) {
        def grid = component.paginatedGrid()
        // Drives a load the way the search field does, which fires the page-loaded event the
        // summary listens to.
        grid.setListState(grid.listState().withFilter(filter).withPage(1))
    }

    private static Span summaryOf(SampleDetailsComponent component) {
        return (Span) descendantsOf(component)
            .find { it instanceof Span && it.getClassNames().contains("sample-result-summary") }
    }

    private static List<Component> descendantsOf(Component root) {
        def found = []
        root.getChildren().forEach { child ->
            found.add(child)
            found.addAll(descendantsOf(child))
        }
        return found
    }

    def "no search result feedback is shown while no search is active"() {
        given: "a loaded sample page with no filter"
        def component = newComponent(299)
        component.paginatedGrid().refresh()

        expect: "the summary stays out of the way, since it would repeat the header's total"
        !summaryOf(component).isVisible()
    }

    def "an active search states how many samples it matches"() {
        given: "a sample page whose search matches three samples"
        def component = newComponent(3)

        when: "a search term is entered"
        applyFilter(component, "Q2IMMU60")

        then: "the number of matches is stated"
        def summary = summaryOf(component)
        summary.isVisible()
        summary.getText() == "3 matching samples"
    }

    def "a single match is stated in the singular"() {
        given: "a sample page whose search matches one sample"
        def component = newComponent(1)

        when: "a search term is entered"
        applyFilter(component, "Q2IMMU60")

        then: "the wording does not read as a plural of one"
        summaryOf(component).getText() == "1 matching sample"
    }

    def "a search that matches nothing leaves the message to the grid's empty state"() {
        given: "a sample page whose search matches no sample"
        def component = newComponent(0)

        when: "a search term is entered"
        applyFilter(component, "NOPE")

        then: "the summary stays hidden rather than repeating the empty state above it"
        // The grid renders "No samples match the current search. Clear the search to see all
        // samples." in the area where the rows would be, which states the same fact and adds the
        // way out. Printing it twice on one screen would be noise.
        !summaryOf(component).isVisible()
    }

    def "clearing the search removes the feedback again"() {
        given: "a sample page with an active search"
        def component = newComponent(3)
        applyFilter(component, "Q2IMMU60")
        assert summaryOf(component).isVisible()

        when: "the search is cleared"
        applyFilter(component, "")

        then: "the summary goes away, so the page returns to its resting shape"
        !summaryOf(component).isVisible()
    }

    def "the feedback is announced to screen readers as a polite status"() {
        given: "a sample page with an active search"
        def component = newComponent(3)

        when: "the summary renders"
        applyFilter(component, "Q2IMMU60")

        then: "the count is a live region, not silent text"
        def element = summaryOf(component).getElement()
        element.getAttribute("role") == "status"
        // Polite, not assertive: the count follows every keystroke, and interrupting the user
        // mid-word would be worse than waiting for a pause.
        element.getAttribute("aria-live") == "polite"
    }
}
