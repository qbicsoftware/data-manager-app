package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.general.MainComponent;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentCreationContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentCreationEvent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
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
@Route(value = "projects/:projectId?/experiments/:experimentId?", layout = ProjectViewPage.class)
@PermitAll
public class ExperimentInformationMain extends MainComponent implements BeforeEnterObserver,
    RouterLayout {

  @Serial
  private static final long serialVersionUID = -3443064087502678981L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final transient ExperimentInformationPageHandler experimentInformationPageHandler;
  private final ProjectNavigationBarComponent projectNavigationBarComponent;

  public ExperimentInformationMain(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ExperimentContentComponent experimentContentComponent,
      @Autowired ExperimentSupportComponent experimentSupportComponent,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    super(experimentContentComponent, experimentSupportComponent);
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(experimentSupportComponent);
    Objects.requireNonNull(experimentContentComponent);
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.projectNavigationBarComponent = projectNavigationBarComponent;
    layoutComponent();
    experimentInformationPageHandler = new ExperimentInformationPageHandler(
        projectNavigationBarComponent, experimentContentComponent, experimentSupportComponent,
        projectInformationService, experimentInformationService);
    log.debug(String.format(
        "\"New instance for ExperimentInformationMain (#%s) created with ProjectNavigationBar Component (#%s), ExperimentMain component (#%s) and ExperimentSupport component (#%s)",
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
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresentOrElse(
            experimentIdParam -> experimentInformationPageHandler.propagateExperimentId(
                experimentIdParam, beforeEnterEvent),
            () -> experimentInformationPageHandler.rerouteToActiveExperiment(beforeEnterEvent));
  }

  /**
   * Triggers the propagation of the provided {@link ProjectId} to the components within
   * {@link ExperimentInformationMain}
   *
   * @param projectId The projectId to be propagated
   */
  public void projectId(ProjectId projectId) {
    experimentInformationPageHandler.setProjectId(projectId);
  }

  private final class ExperimentInformationPageHandler {

    private ProjectId projectId;
    private final ProjectNavigationBarComponent projectNavigationBarComponent;
    private final ExperimentContentComponent experimentContentComponent;
    private final ExperimentSupportComponent experimentSupportComponent;
    private final ProjectInformationService projectInformationService;
    private final ExperimentInformationService experimentInformationService;

    public ExperimentInformationPageHandler(
        ProjectNavigationBarComponent projectNavigationBarComponent,
        ExperimentContentComponent experimentContentComponent,
        ExperimentSupportComponent experimentSupportComponent,
        ProjectInformationService projectInformationService,
        ExperimentInformationService experimentInformationService) {
      this.projectNavigationBarComponent = projectNavigationBarComponent;
      this.experimentContentComponent = experimentContentComponent;
      this.experimentSupportComponent = experimentSupportComponent;
      this.projectInformationService = projectInformationService;
      this.experimentInformationService = experimentInformationService;
      addListeners();
    }

    private void addListeners() {
      enableExperimentSelectionListener();
      enableCreateExperimentListener();
    }

    private void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      propagateProjectInformation();
      propagateExperimentInformation();
    }

    private void propagateExperimentInformation() {
      Collection<Experiment> experiments = getExperimentsForProject(projectId);
      experimentSupportComponent.setExperiments(experiments);
      var activeExperiment = getActiveExperimentIdForProject(projectId);
      experimentSupportComponent.setActiveExperiment(activeExperiment);
      experimentContentComponent.setExperiment(activeExperiment);
    }

    private void propagateProjectInformation() {
      projectNavigationBarComponent.projectId(projectId);
      experimentSupportComponent.projectId(projectId);
    }

    private Collection<Experiment> getExperimentsForProject(ProjectId projectId) {
      var project = projectInformationService.find(projectId);
      return project.<Collection<Experiment>>map(value -> value.experiments().stream()
          .map(experimentInformationService::find).filter(
              Optional::isPresent).map(Optional::get).toList()).orElseGet(ArrayList::new);
    }

    private void setExperimentId(ExperimentId experimentId) {
      experimentContentComponent.setExperiment(experimentId);
      experimentSupportComponent.setSelectedExperiment(experimentId);
      projectNavigationBarComponent.experimentId(experimentId);
    }

    private void enableExperimentSelectionListener() {
      experimentSupportComponent.addExperimentSelectionListener(
          event -> routeToExperiment(projectId, event.getSource().experimentId()));
    }

    private void enableCreateExperimentListener() {
      experimentSupportComponent.addExperimentCreationListener(event -> {
        experimentSupportComponent.setExperiments(getExperimentsForProject(projectId));
        var activeExperiment = getActiveExperimentIdForProject(projectId);
        experimentSupportComponent.setActiveExperiment(activeExperiment);
        experimentContentComponent.setExperiment(retrieveNewlyCreatedExperiment(event));
      });
    }

    private ExperimentId retrieveNewlyCreatedExperiment(
        ExperimentCreationEvent experimentCreationEvent) {
      Project project = projectInformationService.find(projectId).get();
      return project.experiments().stream().map(experimentInformationService::find)
          .filter(Optional::isPresent).map(Optional::get)
          .filter(experiment -> isExperimentEqualToContent(experimentCreationEvent.getSource()
              .content(), experiment)).findFirst().get().experimentId();
    }

    private boolean isExperimentEqualToContent(ExperimentCreationContent experimentCreationContent,
        Experiment experiment) {
      return experimentCreationContent.analytes().equals(experiment.getAnalytes())
          && experimentCreationContent.species().equals(experiment.getSpecies())
          && experimentCreationContent.specimen().equals(experiment.getSpecimens());

    }

    /**
     * Reroutes to the ExperimentId provided in the URL
     * <p>
     * This method generates the URL and routes the user via {@link RouteParam} to the active
     * experiment of a project
     */

    private void propagateExperimentId(String experimentParam, BeforeEnterEvent beforeEnterEvent) {
      try {
        ExperimentId experimentId = ExperimentId.parse(experimentParam);
        experimentInformationPageHandler.setExperimentId(experimentId);
      } catch (IllegalArgumentException e) {
        log.debug(
            String.format("Provided ExperimentId %s is invalid due to %s", experimentParam,
                e.getMessage()));
        rerouteToActiveExperiment(beforeEnterEvent);
      }
    }


    /**
     * Reroutes to the active experiment within a project if present
     * <p>
     * This method generates the URL and routes the user via {@link RouteParam} to the active
     * experiment of a project
     */
    private void rerouteToActiveExperiment(BeforeEnterEvent beforeEnterEvent) {
      ExperimentId activeExperimentId = experimentInformationPageHandler.getActiveExperimentIdForProject(
          projectId);
      log.debug(String.format("Rerouting to active experiment %s of project %s",
          activeExperimentId.value(), projectId.value()));
      RouteParam experimentIdParam = new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
          activeExperimentId.value());
      RouteParam projectIdRouteParam = new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
          projectId.value());
      RouteParameters routeParameters = new RouteParameters(projectIdRouteParam, experimentIdParam);
      beforeEnterEvent.forwardTo(ExperimentInformationMain.class, routeParameters);
    }

    /**
     * Routes to the experiment selected by the user in the component within the
     * {@link ExperimentSupportComponent}
     * <p>
     * This method generates the URL and routes the user via {@link RouteParam} to the selected
     * experiment of a project
     */
    private void routeToExperiment(ProjectId projectId, ExperimentId experimentId) {
      RouteParam experimentIdParam = new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
          experimentId.value());
      RouteParam projectIdRouteParam = new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
          projectId.value());
      RouteParameters routeParameters = new RouteParameters(projectIdRouteParam, experimentIdParam);
      UI.getCurrent()
          .access(() -> UI.getCurrent().navigate(ExperimentInformationMain.class, routeParameters));
    }

    private ExperimentId getActiveExperimentIdForProject(ProjectId projectId) {
      return projectInformationService.find(projectId).get().activeExperiment();
    }
  }

}
