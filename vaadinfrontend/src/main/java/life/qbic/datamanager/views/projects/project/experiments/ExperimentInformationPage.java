package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
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
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment Information page
 * <p>
 * This page hosts the components necessary to show and update the
 * {@link life.qbic.projectmanagement.domain.project.experiment.Experiment} information associated
 * with a {@link life.qbic.projectmanagement.domain.project.Project} via the provided
 * {@link life.qbic.projectmanagement.domain.project.ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?", layout = ProjectViewPage.class)
@PermitAll
public class ExperimentInformationPage extends Div implements BeforeEnterObserver, RouterLayout {

  @Serial
  private static final long serialVersionUID = -3443064087502678981L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  private final transient ExperimentInformationPageHandler experimentInformationPageHandler;

  public ExperimentInformationPage(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ExperimentSupportComponent experimentSupportComponent,
      @Autowired ExperimentMainComponent experimentMainComponent,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(experimentSupportComponent);
    Objects.requireNonNull(experimentMainComponent);
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    setupBoard(projectNavigationBarComponent, experimentMainComponent, experimentSupportComponent);
    experimentInformationPageHandler = new ExperimentInformationPageHandler(
        projectNavigationBarComponent, experimentMainComponent, experimentSupportComponent,
        projectInformationService, experimentInformationService);
    log.debug(String.format(
        "\"New instance for ExperimentInformationPage (#%s) created with ProjectNavigationBar Component (#%s), ExperimentMain component (#%s) and ExperimentSupport component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent),
        System.identityHashCode(experimentMainComponent),
        System.identityHashCode(experimentSupportComponent))
    );
  }

  private void setupBoard(ProjectNavigationBarComponent projectNavigationBarComponent,
      ExperimentMainComponent experimentMainComponent,
      ExperimentSupportComponent experimentSupportComponent) {
    this.addClassName("experiment-page");
    this.add(projectNavigationBarComponent);
    this.add(experimentMainComponent);
    this.add(experimentSupportComponent);
  }

  /**
   * Extracts {@link ExperimentId} from the provided URL before the user accesses the page
   * <p>
   * This method is responsible for checking if the provided {@link ExperimentId} is valid and
   * triggering its propagation to the components within the {@link ExperimentInformationPage}
   */

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .ifPresent(experimentIdParam -> {
          try {
            ExperimentId experimentId = ExperimentId.parse(experimentIdParam);
            experimentInformationPageHandler.setExperimentId(experimentId);
          } catch (IllegalArgumentException e) {
            log.debug(
                String.format("Provided ExperimentId %s is invalid due to %s", experimentIdParam,
                    e.getMessage()));
            experimentInformationPageHandler.rerouteToActiveExperiment(beforeEnterEvent);
          }
        });
  }

  /**
   * Triggers the propagation of the provided {@link ProjectId} to the components within this page
   */
  public void projectId(ProjectId projectId) {
    experimentInformationPageHandler.setProjectId(projectId);
  }

  private final class ExperimentInformationPageHandler {

    private ProjectId projectId;
    private final ProjectNavigationBarComponent projectNavigationBarComponent;
    private final ExperimentMainComponent experimentMainComponent;
    private final ExperimentSupportComponent experimentSupportComponent;
    private final ProjectInformationService projectInformationService;
    private final ExperimentInformationService experimentInformationService;

    public ExperimentInformationPageHandler(
        ProjectNavigationBarComponent projectNavigationBarComponent,
        ExperimentMainComponent experimentMainComponent,
        ExperimentSupportComponent experimentSupportComponent,
        ProjectInformationService projectInformationService,
        ExperimentInformationService experimentInformationService) {
      this.projectNavigationBarComponent = projectNavigationBarComponent;
      this.experimentMainComponent = experimentMainComponent;
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
      projectNavigationBarComponent.projectId(projectId);
      experimentSupportComponent.setProjectId(projectId);
      Collection<Experiment> experiments = getExperimentsForProject(projectId);
      experimentSupportComponent.setExperiments(experiments);
      var activeExperiment = getActiveExperimentIdForProject();
      experimentSupportComponent.setActiveExperiment(activeExperiment);
      experimentMainComponent.setExperiment(activeExperiment);
    }

    private Collection<Experiment> getExperimentsForProject(ProjectId projectId) {
      var project = projectInformationService.find(projectId);
      return project.<Collection<Experiment>>map(value -> value.experiments().stream()
          .map(experimentInformationService::find).filter(
              Optional::isPresent).map(Optional::get).toList()).orElseGet(ArrayList::new);
    }

    private void setExperimentId(ExperimentId experimentId) {
      experimentMainComponent.setExperiment(experimentId);
      experimentSupportComponent.setSelectedExperiment(experimentId);
      projectNavigationBarComponent.experimentId(experimentId);
    }

    private void enableExperimentSelectionListener() {
      experimentSupportComponent.addExperimentSelectionListener(
          event -> routeToSelectedExperiment(projectId, event.getSource().experimentId()));
    }

    private void enableCreateExperimentListener() {
      experimentSupportComponent.addExperimentCreationListener(event -> {
        experimentSupportComponent.setExperiments(getExperimentsForProject(projectId));
        var activeExperiment = getActiveExperimentIdForProject();
        experimentSupportComponent.setActiveExperiment(activeExperiment);
      });
    }

    /**
     * Reroutes to the active experiment within a project if present
     * <p>
     * This method generates the URL and routes the user via {@link RouteParam} to the active
     * experiment of a project
     */
    private void rerouteToActiveExperiment(BeforeEnterEvent beforeEnterEvent) {
      ExperimentId activeExperimentId = experimentInformationPageHandler.getActiveExperimentIdForProject();
      log.debug(String.format("Rerouting to active experiment %s of project %s",
          activeExperimentId.value(), projectId.value()));
      RouteParam experimentIdParam = new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
          activeExperimentId.value());
      RouteParam projectIdRouteParam = new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
          projectId.value());
      RouteParameters routeParameters = new RouteParameters(projectIdRouteParam, experimentIdParam);
      beforeEnterEvent.forwardTo(ExperimentInformationPage.class, routeParameters);
    }

    /**
     * Routes to the experiment selected by the user in the component within the
     * {@link ExperimentSupportComponent}
     * <p>
     * This method generates the URL and routes the user via {@link RouteParam} to the selected
     * experiment of a project
     */
    private void routeToSelectedExperiment(ProjectId projectId, ExperimentId experimentId) {
      RouteParam experimentIdParam = new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
          experimentId.value());
      RouteParam projectIdRouteParam = new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
          projectId.value());
      RouteParameters routeParameters = new RouteParameters(projectIdRouteParam, experimentIdParam);
      UI.getCurrent()
          .access(() -> UI.getCurrent().navigate(ExperimentInformationPage.class, routeParameters));
    }

    private ExperimentId getActiveExperimentIdForProject() {
      return projectInformationService.find(projectId).get().activeExperiment();
    }
  }

}
