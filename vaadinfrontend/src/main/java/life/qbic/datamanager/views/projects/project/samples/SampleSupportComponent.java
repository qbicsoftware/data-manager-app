package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample support component
 * <p>
 * The support component is a {@link Div} container, which is responsible for hosting the components
 * handling the sidebar content within the {@link SampleInformationMain}. It propagates the sample
 * information provided within its components to the {@link SampleInformationMain} and vice versa
 * and can be easily extended with additional components if necessary
 */
@SpringComponent
@UIScope
public class SampleSupportComponent extends Div {

  @Serial
  private static final long serialVersionUID = 6214605184545498061L;
  private static final Logger log = LoggerFactory.logger(SampleSupportComponent.class);
  private final BatchDetailsComponent batchDetailsComponent;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;

  public SampleSupportComponent(@Autowired BatchDetailsComponent batchDetailsComponent,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    this.batchDetailsComponent = batchDetailsComponent;
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    layoutComponent();
  }

  private void layoutComponent() {
    this.add(batchDetailsComponent);
  }

  /**
   * Provides the {@link ProjectId} to the components within this container
   * <p>
   * This method serves as an entry point providing the necessary {@link ProjectId} to components
   * within this component, so they can retrieve the information associated with the
   * {@link ProjectId}
   */
  public void projectId(ProjectId projectId) {
    projectInformationService.find(projectId)
        .ifPresent(project -> propagateExperimentInformation(projectId));
  }

  private void propagateExperimentInformation(ProjectId projectId) {
    Collection<Experiment> experiments = getExperimentsForProject(projectId);
    batchDetailsComponent.setExperiments(experiments);
  }

  private Collection<Experiment> getExperimentsForProject(ProjectId projectId) {
    var project = projectInformationService.find(projectId);
    return project.<Collection<Experiment>>map(value -> value.experiments().stream()
        .map(experimentInformationService::find).filter(Optional::isPresent).map(Optional::get)
        .toList()).orElseGet(ArrayList::new);
  }

}
