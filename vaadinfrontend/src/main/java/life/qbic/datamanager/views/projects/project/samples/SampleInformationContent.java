package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@SpringComponent
@UIScope
@PermitAll
public class SampleInformationContent extends Div {

  @Serial
  private static final long serialVersionUID = -5431288053780884294L;
  private static final Logger log = LoggerFactory.logger(SampleInformationContent.class);
  private final SampleOverviewComponent sampleOverviewComponent;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;

  public SampleInformationContent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleOverviewComponent sampleOverviewComponent) {
    this.addClassName("sample-content");
    this.sampleOverviewComponent = sampleOverviewComponent;
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
  }

  public void projectId(ProjectId projectId) {
    projectInformationService.find(projectId)
        .ifPresentOrElse(this::loadExperimentInformation, this::displayProjectNotFound);
    propagateProjectInformation(projectId);
  }

  public void loadExperimentInformation(Project project) {
    Collection<Experiment> foundExperiments = new ArrayList<>();
    project.experiments().forEach(experimentId -> experimentInformationService.find(experimentId)
        .ifPresent(foundExperiments::add));
    if (!foundExperiments.isEmpty()) {
      propagateExperimentInformation(foundExperiments);
      displayComponentInContent(sampleOverviewComponent);
    } else {
      displayNoExperimentsFound();
    }
  }

  private void propagateProjectInformation(ProjectId projectId) {
    sampleOverviewComponent.setProject(projectId);
  }

  private void propagateExperimentInformation(Collection<Experiment> experiments) {
    sampleOverviewComponent.setExperiments(experiments);
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

  private void displayNoExperimentsFound() {
    ErrorMessage errorMessage = new ErrorMessage("No Experiments defined",
        "No Experiments are defined in project");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  private void displayProjectNotFound() {
    ErrorMessage errorMessage = new ErrorMessage("Project not found",
        "Please try to reload the page");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

}
