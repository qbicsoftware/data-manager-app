package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.MainComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Information Main Component
 * <p>
 * This component hosts the components necessary to show and update the information for all
 * {@link Sample} associated with all {@link Experiment} of a {@link Project} information via the
 * provided {@link ProjectId} in the URL
 */

@Route(value = Projects.SAMPLES, layout = ExperimentMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class SampleInformationMain extends MainComponent implements BeforeEnterObserver,
    RouterLayout {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(SampleInformationMain.class);
  private final SampleContentComponent sampleContentComponent;
  private final SampleSupportComponent sampleSupportComponent;
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private transient Context context;

  public SampleInformationMain(
      @Autowired SampleContentComponent sampleContentComponent,
      @Autowired SampleSupportComponent sampleSupportComponent) {
    super(sampleContentComponent, sampleSupportComponent);
    Objects.requireNonNull(sampleContentComponent);
    Objects.requireNonNull(sampleSupportComponent);
    this.sampleContentComponent = sampleContentComponent;
    this.sampleSupportComponent = sampleSupportComponent;
    layoutComponent();
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        sampleContentComponent.getClass().getSimpleName(),
        System.identityHashCode(sampleContentComponent),
        sampleSupportComponent.getClass().getSimpleName(),
        System.identityHashCode(sampleSupportComponent)));
  }

  private void layoutComponent() {
    addClassName("sample");
  }

  /**
   * Provides the {@link Context} to the components within this page
   * <p>
   * This method serves as an entry point providing the necessary {@link Context} to the components
   * within this cage
   *
   * @param context Context containing the projectId of the selected project
   */
  public void setContext(Context context) {
    sampleContentComponent.setContext(context);
    sampleSupportComponent.projectId(context.projectId().orElseThrow());
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectID = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    context = new Context().with(parsedProjectId);
    String experimentId = event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ExperimentId.isValid(experimentId)) {
      throw new ApplicationException("invalid experiment id " + experimentId);
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    this.context = context.with(parsedExperimentId);
    setContext(context);
  }
}
