package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;

/**
 * Routes the two retired measurement routes into the merged measurement dataset view.
 *
 * <p>{@code .../measurements} and {@code .../rawdata} were the two halves of one subject. They are
 * kept as redirects rather than deleted outright because bookmarks, shared links and any external
 * documentation still point at them, and a 404 on a previously valid deep link is a worse outcome
 * than one extra hop.</p>
 *
 * <p>Each subclass declares one legacy path and inherits the redirect. They are separate classes
 * because a {@code @Route} value is static per class.</p>
 *
 * @since 1.21.0
 */
abstract class LegacyMeasurementRoutesRedirect implements BeforeEnterObserver {

  static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  /**
   * Redirects to the merged view, carrying the project and experiment identity so the reader lands
   * on the same experiment they asked for.
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    event.rerouteTo(MeasurementDatasetMain.class, new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
            event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
            event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER).orElseThrow())));
  }

  /** The retired measurements route. */
  @UIScope
  @SpringComponent
  @Route(value = "projects/:projectId?/experiments/:experimentId?/measurements",
      layout = ProjectMainLayout.class)
  @PermitAll
  static class MeasurementsRedirect extends LegacyMeasurementRoutesRedirect {
  }

  /** The retired raw-data route. */
  @UIScope
  @SpringComponent
  @Route(value = "projects/:projectId?/experiments/:experimentId?/rawdata",
      layout = ProjectMainLayout.class)
  @PermitAll
  static class RawDataRedirect extends LegacyMeasurementRoutesRedirect {

  }
}
