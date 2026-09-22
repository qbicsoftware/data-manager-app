package life.qbic.datamanager.views.projects.project.samples

import com.vaadin.flow.router.QueryParameters
import life.qbic.application.commons.SortOrder
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.general.pagination.ListState
import life.qbic.datamanager.views.general.pagination.ListStateCodec
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.projectmanagement.application.api.AsyncProjectService
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.ProjectId
import org.springframework.context.support.StaticMessageSource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.util.Set

/**
 * Regression guard for the URL list-state synchronisation on the samples route (USER-R-03,
 * FEAT-PAG-LIST-02). The route parses {@code page/size/q/sort} from the URL and must apply those
 * parameters to the shown paginated grid.
 */
class SampleDetailsUrlStateSpec extends Specification {

    private SampleDetailsComponent newComponent(int totalSamples = 1000) {
        def messageSource = new StaticMessageSource()
        def messages = new MessageSourceNotificationFactory(messageSource)
        def service = Stub(AsyncProjectService) {
            countSamples(_, _, _) >> Mono.just(totalSamples)
            getSamplePreviews(_, _, _, _, _) >> Flux.empty()
        }
        def context = new Context()
            .with(ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5"))
            .with(ExperimentId.parse("3270ce7f-4092-40e3-9c4c-ce7adb688bf8"))
            .withProjectCode("QABCDE")
        return new SampleDetailsComponent(service, messages, context)
    }

    def "URL page, size, filter and sort parameters are applied to the grid on navigation"() {
        given: "a component for the current project/experiment"
        def component = newComponent()

        and: "the URL carries size=24, page=2, q=cancer and a sample name sort"
        def urlState = ListStateCodec.parse(
            QueryParameters.fromString("size=24&page=2&q=cancer&sort=sampleName:asc"),
            SampleSort.DEFAULT,
            SampleSort.allowedSortOrders())

        when: "the URL state is applied"
        component.applyExternalState(urlState)

        then: "the applied list state reflects every URL parameter"
        component.listState().page() == 2
        component.listState().pageSize() == 24
        component.listState().filter() == "cancer"
        component.listState().sort() == new SortOrder("sampleName", false)
    }

    def "absent URL parameters fall back to the sample defaults"() {
        given:
        def component = newComponent()

        when: "the URL carries only the sort"
        def urlState = ListStateCodec.parse(
            QueryParameters.fromString("sort=sampleId:asc"),
            SampleSort.DEFAULT,
            SampleSort.allowedSortOrders())
        component.applyExternalState(urlState)

        then: "page one and the default page size apply"
        component.listState().page() == 1
        component.listState().pageSize() == ListStateCodec.DEFAULT_PAGE_SIZE
        component.listState().sort() == SampleSort.DEFAULT
    }

    def "out-of-range page is clamped to the last valid page"() {
        given: "only 5 samples exist and the page size is 12, so a single page exists"
        def component = newComponent(5)

        when: "a page beyond the result set is requested"
        component.applyExternalState(new ListState(5, 12, "", SampleSort.DEFAULT))

        then: "the page is clamped to page one"
        component.listState().page() == 1
    }

    def "cross-page selection of sample ids survives a page change"() {
        given: "a component with enough samples for several pages"
        def component = newComponent(100)

        and: "two sample ids are selected"
        component.paginatedGrid().select(Set.of("3270ce7f-4092-40e3-9c4c-ce7adb688b01",
            "3270ce7f-4092-40e3-9c4c-ce7adb688b02"))

        when: "a different page is loaded"
        component.applyExternalState(new ListState(2, 12, "", SampleSort.DEFAULT))

        then: "the selection is preserved across the page render"
        component.paginatedGrid().selectedIds() == Set.of("3270ce7f-4092-40e3-9c4c-ce7adb688b01",
            "3270ce7f-4092-40e3-9c4c-ce7adb688b02")
    }
}