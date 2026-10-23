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
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDesignComponent;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment design editor.
 *
 * <p>The design editor for an experiment, at
 * {@code projects/:projectId/experiments/:experimentId/design}. It is a real, URL-addressable route
 * reached from the overview's edit action — not a dialog and not an in-place toggle (project owner,
 * 2026-10-19). The editing itself is inline; see {@link ExperimentDesignComponent}.</p>
 *
 * <p>Its sibling {@link ExperimentSetupMain} holds the experiment name and sample origins. They are
 * separate routes because the design used to sit below that setup form on one page, so a user who
 * came to edit the design had to scroll past a section they did not need.</p>
 *
 * @since 1.19.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/design",
    layout = ProjectMainLayout.class)
@PermitAll
public class ExperimentDesignMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  private final ExperimentDesignComponent experimentDesignComponent;
  private transient Context context;

  public ExperimentDesignMain(
      @Autowired ExperimentDesignComponent experimentDesignComponent) {
    this.experimentDesignComponent = requireNonNull(experimentDesignComponent);
    addClassName("experiment-design");
    add(experimentDesignComponent);
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
    experimentDesignComponent.setContext(context);
  }
}
