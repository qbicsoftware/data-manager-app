package life.qbic.datamanager.views.projects.project.experiments;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.general.MainComponent;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the
 * {@link life.qbic.projectmanagement.domain.project.experiment.Experiment} information associated
 * with a {@link life.qbic.projectmanagement.domain.project.Project} via the provided
 * {@link life.qbic.projectmanagement.domain.project.ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?", layout = MainLayout.class)
@PermitAll
public class ExperimentInformationMain extends MainComponent implements BeforeEnterObserver,
    RouterLayout {

  @Serial
  private static final long serialVersionUID = -3443064087502678981L;
  private static final Logger log = logger(ExperimentInformationMain.class);
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final ProjectNavigationBarComponent projectNavigationBarComponent;
  private final ExperimentContentComponent experimentContentComponent;
  private final ExperimentSupportComponent experimentSupportComponent;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private Context context;

  public ExperimentInformationMain(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ExperimentContentComponent experimentContentComponent,
      @Autowired ExperimentSupportComponent experimentSupportComponent,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    super(experimentContentComponent, experimentSupportComponent);
    requireNonNull(projectNavigationBarComponent);
    requireNonNull(experimentSupportComponent);
    requireNonNull(experimentContentComponent);
    requireNonNull(projectInformationService);
    requireNonNull(experimentInformationService);
    this.projectNavigationBarComponent = projectNavigationBarComponent;
    this.experimentContentComponent = experimentContentComponent;
    this.experimentSupportComponent = experimentSupportComponent;
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    layoutComponent();
    log.debug(String.format(
        "New instance for ExperimentInformationMain (#%s) created with ProjectNavigationBar Component (#%s), ExperimentMain component (#%s) and ExperimentSupport component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent),
        System.identityHashCode(experimentContentComponent),
        System.identityHashCode(experimentSupportComponent))
    );
  }

  private void layoutComponent() {
    addClassName("experiment");
    addComponentAsFirst(projectNavigationBarComponent);
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

    if (beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER).isEmpty()) {
      Project project = projectInformationService.find(parsedProjectId)
          .orElseThrow();
      project.experiments().stream()
          .findFirst()
          .ifPresent(experimentId -> forwardToExperiment(experimentId, beforeEnterEvent));
      setContext(this.context);
      return; // abort the before-enter event
    }

    String experimentId = beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow();

    if (!ExperimentId.isValid(experimentId)) {
      beforeEnterEvent.rerouteToError(NotFoundException.class);
      return;
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    if (experimentInformationService.find(parsedExperimentId).isEmpty()) {
      beforeEnterEvent.rerouteToError(NotFoundException.class);
      return;
    }
    this.context = this.context
        .with(parsedExperimentId);

    setContext(this.context);
  }

  private void setContext(Context context) {
    experimentSupportComponent.setContext(context);
    experimentContentComponent.setContext(context);
    projectNavigationBarComponent.projectId(context.projectId().orElseThrow());
    this.context = context;
  }

  private void forwardToExperiment(ExperimentId experimentId, BeforeEnterEvent beforeEnterEvent) {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
            context.projectId().map(ProjectId::value).orElseThrow()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER, experimentId.value()));
    log.debug("Forwarding to experiment "
        + experimentId.value());
    beforeEnterEvent.forwardTo(ExperimentInformationMain.class, routeParameters);
  }

}
