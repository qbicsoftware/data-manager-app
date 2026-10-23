package life.qbic.datamanager.views.projects.project.experiments;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentSetupComponent;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment setup surface.
 *
 * <p>The editing surface for an experiment's name and sample origins, at
 * {@code projects/:projectId/experiments/:experimentId/setup}. Its sibling
 * {@link ExperimentDesignMain} holds the design.</p>
 *
 * <p>The two are separate routes because they are separate jobs that were previously stacked on one
 * page: the origins exist so the sample-registration template has its Species, Specimen and Analyte
 * options, while the design holds the variables and groups that produce the template's Condition
 * option. On one route the design sat below the whole setup form, so a user arriving to edit the
 * design had to scroll past a section they did not need (project owner, 2026-10-19).</p>
 *
 * @since 1.19.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/setup",
    layout = ProjectMainLayout.class)
@PermitAll
public class ExperimentSetupMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  private final ExperimentSetupComponent experimentSetupComponent;
  private transient Context context;

  public ExperimentSetupMain(@Autowired ExperimentSetupComponent experimentSetupComponent) {
    this.experimentSetupComponent = requireNonNull(experimentSetupComponent);
    addClassName("experiment-setup");
    add(experimentSetupComponent);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectIdValue = beforeEnterEvent.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing project id in route"));
    if (!ProjectId.isValid(projectIdValue)) {
      throw new ApplicationException("invalid project id " + projectIdValue);
    }
    ProjectId projectId = ProjectId.parse(projectIdValue);

    String experimentIdValue = beforeEnterEvent.getRouteParameters()
        .get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing experiment id in route"));
    if (!ExperimentId.isValid(experimentIdValue)) {
      throw new ApplicationException("invalid experiment id " + experimentIdValue);
    }
    ExperimentId experimentId = ExperimentId.parse(experimentIdValue);

    this.context = new Context().with(projectId).with(experimentId);
    experimentSetupComponent.setContext(context);
  }
}
