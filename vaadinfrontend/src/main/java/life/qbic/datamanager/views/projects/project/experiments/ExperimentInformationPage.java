package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent;
import life.qbic.datamanager.views.support.experiment.ExperimentItem;
import life.qbic.datamanager.views.support.experiment.ExperimentItemClickedEvent;
import life.qbic.datamanager.views.support.experiment.ExperimentItemCollection;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
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

  private ExperimentItemCollection itemCollection;
  @Serial
  private static final long serialVersionUID = -3443064087502678981L;
  private static final Logger log = LoggerFactory.logger(ProjectViewPage.class);
  private final transient ExperimentInformationPageHandler experimentInformationPageHandler;
  private final ExperimentInformationService experimentInformationService;

  public ExperimentInformationPage(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired ExperimentDetailsComponent experimentDetailsComponent,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(experimentDetailsComponent);
    Objects.requireNonNull(projectInformationService);
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    setupBoard(projectNavigationBarComponent, experimentDetailsComponent);
    experimentInformationPageHandler = new ExperimentInformationPageHandler(
        projectNavigationBarComponent, experimentDetailsComponent,
        projectInformationService);
    log.debug(String.format(
        "\"New instance for Experiment Information page (#%s) created with Project Navigation Bar Component (#%s) and Experiment Details Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent),
        System.identityHashCode(experimentDetailsComponent)
      ));
  }

  private void setupBoard(ProjectNavigationBarComponent projectNavigationBarComponent,
      ExperimentDetailsComponent experimentDetailsComponent) {
    Board board = new Board();

    Row rootRow = new Row();

    VerticalLayout mainComponents = new VerticalLayout();
    mainComponents.setPadding(false);
    mainComponents.setMargin(false);
    mainComponents.setSpacing(false);
    mainComponents.add(projectNavigationBarComponent, experimentDetailsComponent);

    rootRow.add(mainComponents, 3);
    itemCollection = ExperimentItemCollection.create("Add a new experiment");
    rootRow.add(itemCollection, 1);

    board.add(rootRow);

    board.setSizeFull();

    add(board);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    beforeEnterEvent.getRouteParameters().get("experimentId").ifPresent(experimentIdParam -> {
      try {
        ExperimentId experimentId = ExperimentId.parse(experimentIdParam);
        experimentInformationPageHandler.setExperimentId(experimentId);
      } catch (IllegalArgumentException e) {
        log.debug(String.format("Provided ExperimentId %s is invalid due to %s", experimentIdParam,
            e.getMessage()));
        experimentInformationPageHandler.rerouteToActiveExperiment(beforeEnterEvent);
      }
    });
  }

  public void projectId(ProjectId projectId) {
    experimentInformationPageHandler.setProjectId(projectId);
  }

  private final class ExperimentInformationPageHandler {

    private ProjectId projectId;
    private final ProjectNavigationBarComponent projectNavigationBarComponent;
    private final ExperimentDetailsComponent experimentDetailsComponent;
    private final ProjectInformationService projectInformationService;

    public ExperimentInformationPageHandler(
        ProjectNavigationBarComponent projectNavigationBarComponent,
        ExperimentDetailsComponent experimentDetailsComponent,
        ProjectInformationService projectInformationService) {
      this.projectNavigationBarComponent = projectNavigationBarComponent;
      this.experimentDetailsComponent = experimentDetailsComponent;
      this.projectInformationService = projectInformationService;
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      projectNavigationBarComponent.projectId(projectId);
      itemCollection.removeAll();
      var project = projectInformationService.find(projectId);
      if (project.isEmpty()) {
        return;
      }
      project.get().experiments().stream()
          .map(experimentInformationService::find).filter(
              Optional::isPresent).forEach(experiment -> {
            ExperimentInformationPage.this.itemCollection.addExperimentItem(
                ExperimentItem.create(experiment.get()));
          });
      var activeExperiment = project.get().activeExperiment();
      itemCollection.findBy(activeExperiment).ifPresent(ExperimentItem::setAsActive);
      enableActiveExperimentSelectionListener();
    }

    private void enableActiveExperimentSelectionListener() {
      itemCollection.addClickEventListener((ComponentEventListener<ExperimentItemClickedEvent>) event -> {
        var newActiveExperiment = event.getSource().experimentId();
        routeToSelectedExperiment(projectId, newActiveExperiment);
      });
    }
    
    private void setSelectedExperiment(ExperimentId newActiveExperiment) {
      projectInformationService.setActiveExperiment(projectId, newActiveExperiment);
    }

    public void setExperimentId(ExperimentId experimentId) {
      itemCollection.findBy(experimentId).ifPresent(ExperimentItem::setAsSelected);
      experimentDetailsComponent.setExperiment(experimentId);
      projectNavigationBarComponent.experimentId(experimentId);
    }

    public void rerouteToActiveExperiment(BeforeEnterEvent beforeEnterEvent) {
      ExperimentId activeExperimentId = experimentInformationPageHandler.getActiveExperimentIdForProject();
      log.debug(String.format("Rerouting to active experiment %s of project %s",
          activeExperimentId.value(), projectId.value()));
      RouteParam experimentIdParam = new RouteParam("experimentId", activeExperimentId.value());
      RouteParam projectIdRouteParam = new RouteParam("projectId", projectId.value());
      RouteParameters routeParameters = new RouteParameters(projectIdRouteParam, experimentIdParam);
      beforeEnterEvent.forwardTo(ExperimentInformationPage.class, routeParameters);
    }

    public void routeToSelectedExperiment(ProjectId projectId, ExperimentId experimentId) {
      RouteParam experimentIdParam = new RouteParam("experimentId", experimentId.value());
      RouteParam projectIdRouteParam = new RouteParam("projectId", projectId.value());
      RouteParameters routeParameters = new RouteParameters(projectIdRouteParam, experimentIdParam);
      UI.getCurrent().access(() -> UI.getCurrent().navigate(ExperimentInformationPage.class, routeParameters));
    }

    public ExperimentId getActiveExperimentIdForProject() {
      return projectInformationService.find(projectId).get().activeExperiment();
    }
  }

}
