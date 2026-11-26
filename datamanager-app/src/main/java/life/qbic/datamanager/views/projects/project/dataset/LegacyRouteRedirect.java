package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

/**
 * Redirects a retired route to the merged {@link MeasurementDatasetMain}.
 *
 * <p>{@code .../measurements} and {@code .../rawdata} were the two halves of one subject and are
 * no longer separate surfaces. They are kept as redirects rather than deleted outright, because
 * bookmarks, shared links and external documentation still point at them, and a 404 on a previously
 * valid deep link is worse than one extra hop.</p>
 *
 * <p>This extends {@link Div} because Vaadin requires a {@code @Route} class to be a {@code
 * Component}. Nothing is rendered: the view reroutes before the component tree is attached.</p>
 *
 * @since 1.21.0
 */
abstract class LegacyRouteRedirect extends Div implements BeforeEnterObserver {

  static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  /**
   * Reroutes to the merged view, carrying project and experiment identity so the reader lands on
   * the experiment they asked for.
   *
   * <p>The old list state (page, filter, sort) is deliberately not carried over: the merged view
   * has its own state vocabulary and segment filter, and translating between them would guess.
   * Identity is what a bookmark needs to keep working.</p>
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectId = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    String experimentId = event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    event.rerouteTo(MeasurementDatasetMain.class,
        new com.vaadin.flow.router.RouteParameters(
            new com.vaadin.flow.router.RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId),
            new com.vaadin.flow.router.RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER, experimentId)));
  }
}
