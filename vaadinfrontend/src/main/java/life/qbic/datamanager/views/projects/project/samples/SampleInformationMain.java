package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.general.MainComponent;
import life.qbic.datamanager.views.projects.project.ProjectNavigationBarComponent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the information for all
 * {@link life.qbic.projectmanagement.domain.project.sample.Sample} associated with all
 * {@link Experiment} of a {@link life.qbic.projectmanagement.domain.project.Project} information
 * via the provided {@link life.qbic.projectmanagement.domain.project.ProjectId} in the URL
 */

@Route(value = "projects/:projectId?/samples", layout = MainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class SampleInformationMain extends MainComponent implements BeforeEnterObserver,
    RouterLayout {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(SampleInformationMain.class);
  private final ProjectNavigationBarComponent projectNavigationBarComponent;
  private final SampleContentComponent sampleContentComponent;
  private final SampleSupportComponent sampleSupportComponent;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";

  public SampleInformationMain(
      @Autowired ProjectNavigationBarComponent projectNavigationBarComponent,
      @Autowired SampleContentComponent sampleContentComponent,
      @Autowired SampleSupportComponent sampleSupportComponent) {
    super(sampleContentComponent, sampleSupportComponent);
    Objects.requireNonNull(projectNavigationBarComponent);
    Objects.requireNonNull(sampleContentComponent);
    Objects.requireNonNull(sampleSupportComponent);
    this.projectNavigationBarComponent = projectNavigationBarComponent;
    this.sampleContentComponent = sampleContentComponent;
    this.sampleSupportComponent = sampleSupportComponent;
    layoutComponent();
    log.debug(String.format(
        "\"New instance for Sample Information page (#%s) created with Project Navigation Bar Component (#%s), Sample Content Component (#%s) and Sample Support Component (#%s)",
        System.identityHashCode(this), System.identityHashCode(projectNavigationBarComponent),
        System.identityHashCode(sampleContentComponent),
        System.identityHashCode(sampleSupportComponent)));
  }

  private void layoutComponent() {
    addClassName("sample");
    addComponentAsFirst(projectNavigationBarComponent);
  }

  /**
   * Provides the {@link ProjectId} to the components within this page
   * <p>
   * This method serves as an entry point providing the necessary {@link ProjectId} to the
   * components within this cage
   *
   * @param projectId projectId of the selected project
   */
  public void projectId(ProjectId projectId) {
    projectNavigationBarComponent.projectId(projectId);
    sampleContentComponent.projectId(projectId);
    sampleSupportComponent.projectId(projectId);
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .ifPresent(this::propagateProjectId);
  }

  /**
   * Reroutes to the ProjectId provided in the URL
   * <p>
   * This method generates the URL and routes the user via {@link RouteParam} to the provided
   * ProjectId
   */
  private void propagateProjectId(String projectParam) {
    try {
      ProjectId projectId = ProjectId.parse(projectParam);
      projectId(projectId);
    } catch (IllegalArgumentException e) {
      log.debug(
          String.format("Provided ProjectId %s is invalid due to %s", projectParam,
              e.getMessage()));
    }
  }
}
