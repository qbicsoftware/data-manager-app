package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.samples.SampleDetailsComponent.BatchRegistrationListener;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample main component
 * <p>
 * The main component is a {@link Div} container, which is responsible for hosting the components
 * handling the main content within the {@link SampleInformationMain}. It propagates the
 * {@link Project} and {@link Experiment} to the components within this container. Additionally, it
 * propagates the {@link Batch} and {@link Sample} information provided in the
 * {@link SampleDetailsComponent} to the {@link SampleInformationMain} and can be easily extended
 * with additional components.
 */
@SpringComponent
@UIScope
@PermitAll
public class SampleContentComponent extends Div {

  @Serial
  private static final long serialVersionUID = -5431288053780884294L;
  private ProjectId projectId;
  private static final Logger log = LoggerFactory.logger(SampleContentComponent.class);
  private final SampleDetailsComponent sampleDetailsComponent;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;

  public SampleContentComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleDetailsComponent sampleDetailsComponent) {
    this.sampleDetailsComponent = sampleDetailsComponent;
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    reloadOnBatchRegistration();
  }

  /**
   * Provides the {@link ProjectId} to the components within this container
   * <p>
   * This method serves as an entry point providing the necessary {@link ProjectId} to components
   * within this container
   *
   * @param projectId projectId of the selected project
   */
  public void projectId(ProjectId projectId) {
    this.projectId = projectId;
    projectInformationService.find(projectId)
        .ifPresentOrElse(project -> {
          propagateProjectInformation(projectId);
          propagateExperimentInformation(projectId);
        }, this::displayProjectNotFound);
  }

  private void propagateProjectInformation(ProjectId projectId) {
    sampleDetailsComponent.setProject(projectId);
  }

  private void propagateExperimentInformation(ProjectId projectId) {
    Collection<Experiment> experiments = getExperimentsForProject(projectId);
    if (experiments.isEmpty()) {
      displayNoExperimentsFound();
    } else {
      displayComponentInContent(sampleDetailsComponent);
      sampleDetailsComponent.setExperiments(experiments);
    }
  }

  private Collection<Experiment> getExperimentsForProject(ProjectId projectId) {
    var project = projectInformationService.find(projectId);
    return project.<Collection<Experiment>>map(value -> value.experiments().stream()
        .map(experimentInformationService::find).filter(Optional::isPresent).map(Optional::get)
        .toList()).orElseGet(ArrayList::new);
  }

  private boolean isComponentInContent(Component component) {
    return this.getChildren().collect(Collectors.toSet()).contains(component);
  }

  private void displayComponentInContent(Component component) {
    if (!isComponentInContent(component)) {
      this.removeAll();
      this.add(component);
    }
  }

  /**
   * Propagates the listener which will retrieve notification if a new {@link Batch} was created in
   * the {@link SampleDetailsComponent} within this container
   *
   * @param batchRegistrationListener listener to be notified if a batchRegistration happened in the
   *                                  {@link SampleDetailsComponent}
   */
  public void addBatchRegistrationListener(BatchRegistrationListener batchRegistrationListener) {
    sampleDetailsComponent.addBatchRegistrationListener(batchRegistrationListener);
  }

  private void reloadOnBatchRegistration() {
    sampleDetailsComponent.addBatchRegistrationListener(
        event -> propagateExperimentInformation(projectId));
  }

  private void displayNoExperimentsFound() {
    this.removeAll();
    ErrorMessage errorMessage = new ErrorMessage("No Experiments defined",
        "No Experiments are defined in project");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  private void displayProjectNotFound() {
    this.removeAll();
    ErrorMessage errorMessage = new ErrorMessage("Project not found",
        "Please try to reload the page");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }
}
