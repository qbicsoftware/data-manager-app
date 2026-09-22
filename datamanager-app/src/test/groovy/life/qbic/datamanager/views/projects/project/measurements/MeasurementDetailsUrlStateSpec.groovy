package life.qbic.datamanager.views.projects.project.measurements

import com.vaadin.flow.router.QueryParameters
import life.qbic.datamanager.views.Context
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementDomain
import life.qbic.datamanager.views.projects.project.measurements.pagination.MeasurementListStateCodec
import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId
import life.qbic.projectmanagement.domain.model.project.ProjectId
import org.springframework.context.support.StaticMessageSource
import spock.lang.Specification

import java.util.stream.Stream

/**
 * Regression guard for the URL list-state synchronisation on the measurements route (USER-R-03,
 * ADR-0008). The route parses {@code tab/page/size/q/sort} from the URL and must apply those
 * parameters to the shown grid; previously {@code setContext} reset the per-tab state to defaults
 * after the URL state had been applied, so the URL parameters (e.g. {@code size=24&page=23}) never
 * reached the grid.
 */
class MeasurementDetailsUrlStateSpec extends Specification {

    /**
     * A component whose NGS lookup reports enough measurements for page 23 of size 24 to exist, so
     * the requested page is not clamped away during the fetch.
     */
    private MeasurementDetailsComponent newComponentWithNgsMeasurements() {
        def messageSource = new StaticMessageSource()
        def messages = new MessageSourceNotificationFactory(messageSource)
        def ngs = Stub(NgsMeasurementLookup) {
            countNgsMeasurements(_, _) >> 1000
            lookupNgsMeasurements(_, _, _, _, _) >> Stream.empty()
        }
        return new MeasurementDetailsComponent(messages, ngs,
                Stub(PxpMeasurementLookup),
                Stub(IpMeasurementLookup))
    }

    def "URL tab, page and size parameters are applied to the shown grid on navigation"() {
        given: "a component and a context for the current project/experiment"
        def component = newComponentWithNgsMeasurements()
        def context = new Context()
            .with(ProjectId.parse("0270ce7f-4092-40e3-9c4c-ce7adb688bf5"))
            .with(ExperimentId.parse("3270ce7f-4092-40e3-9c4c-ce7adb688bf8"))

        and: "the URL carries tab=ngs, size=24 and page=23"
        def urlState = MeasurementListStateCodec.parse(
            QueryParameters.fromString("tab=ngs&size=24&page=23"),
            component.getTabPagination().listState())

        when: "the beforeEnter sequence runs: the context is set (resets to defaults), then the URL state is applied"
        component.setContext(context)
        component.getTabPagination().applyExternalState(urlState)

        then: "the URL parameters are honoured instead of being reset to defaults"
        component.getTabPagination().activeTab() == MeasurementDomain.NGS
        component.getTabPagination().listState().stateOf(MeasurementDomain.NGS).page() == 23
        component.getTabPagination().listState().stateOf(MeasurementDomain.NGS).pageSize() == 24
    }
}