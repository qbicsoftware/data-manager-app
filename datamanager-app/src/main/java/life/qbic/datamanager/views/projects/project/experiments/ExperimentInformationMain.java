package life.qbic.datamanager.views.projects.project.experiments;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentOverviewComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.CreateExperimentMain;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment landing.
 *
 * <p>Renders the read-only {@link ExperimentOverviewComponent}: design first, then the data surfaces.
 * The design editor stays at {@code /design}, reachable from the overview (project owner,
 * 2026-10-19).</p>
 *
 * @since 1.19.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?", layout = ProjectMainLayout.class)
@PermitAll
public class ExperimentInformationMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -3443064087502678981L;

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  private final ExperimentOverviewComponent overviewComponent;
  private transient Context context;

  public ExperimentInformationMain(@Autowired ExperimentOverviewComponent overviewComponent) {
    this.overviewComponent = requireNonNull(overviewComponent);
    addClassName("experiment");
    add(overviewComponent);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectIdValue = beforeEnterEvent.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing project id in route"));
    if (!ProjectId.isValid(projectIdValue)) {
      throw new ApplicationException("invalid project id " + projectIdValue);
    }
    String experimentIdValue = beforeEnterEvent.getRouteParameters()
        .get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing experiment id in route"));
    // Defensive guard against the static "create" segment matching this parameter route.
    if ("create".equals(experimentIdValue)) {
      beforeEnterEvent.rerouteTo(CreateExperimentMain.class, new RouteParameters(
          new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectIdValue)));
      return;
    }
    if (!ExperimentId.isValid(experimentIdValue)) {
      throw new ApplicationException("invalid experiment id " + experimentIdValue);
    }
    this.context = new Context().with(ProjectId.parse(projectIdValue))
        .with(ExperimentId.parse(experimentIdValue));
    overviewComponent.setContext(context);
  }
}
