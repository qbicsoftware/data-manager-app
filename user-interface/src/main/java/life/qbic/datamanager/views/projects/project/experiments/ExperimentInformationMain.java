package life.qbic.datamanager.views.projects.project.experiments;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

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
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the {@link Experiment}
 * information associated with a {@link Project} via the provided {@link ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?", layout = ExperimentMainLayout.class)
@PermitAll
public class ExperimentInformationMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -3443064087502678981L;
  private static final Logger log = logger(ExperimentInformationMain.class);
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ExperimentDetailsComponent experimentDetailsComponent;
  private transient Context context;

  public ExperimentInformationMain(
      @Autowired ExperimentDetailsComponent experimentDetailsComponent) {
    requireNonNull(experimentDetailsComponent);
    this.experimentDetailsComponent = experimentDetailsComponent;
    addClassName("experiment");
    add(experimentDetailsComponent);
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        experimentDetailsComponent.getClass().getSimpleName(),
        System.identityHashCode(experimentDetailsComponent)
    ));
  }


  /**
   * Extracts {@link ExperimentId} from the provided URL before the user accesses the page
   * <p>
   * This method is responsible for checking if the provided {@link ExperimentId} is valid and
   * triggering its propagation to the components within the {@link ExperimentInformationMain}
   */

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectID = beforeEnterEvent.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    this.context = new Context().with(parsedProjectId);
    String experimentId = beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ExperimentId.isValid(experimentId)) {
      throw new ApplicationException("invalid experiment id " + experimentId);
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    this.context = context.with(parsedExperimentId);
    initializeComponentsWithContext();
  }

  private void initializeComponentsWithContext() {
    experimentDetailsComponent.setContext(context);
    add(experimentDetailsComponent);
  }
}
