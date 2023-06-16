package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.JustifyContent;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.SampleInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
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
public class SampleInformationContent extends PageArea {

  @Serial
  private static final long serialVersionUID = -5431288053780884294L;
  private static final Logger log = LoggerFactory.logger(SampleInformationContent.class);
  private final SampleOverviewComponent sampleOverviewComponent;
  private final VerticalLayout noExperimentalGroupInProject = new VerticalLayout();
  private final VerticalLayout noSamplesInProject = new VerticalLayout();
  private ProjectId projectId;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient SampleInformationService sampleInformationService;

  public SampleInformationContent(@Autowired SampleOverviewComponent sampleOverviewComponent,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleInformationService sampleInformationService) {
    this.addClassName("sample-content-area");
    initNoSampleRegistered();
    initNoExperimentalGroupsDefined();
    this.sampleOverviewComponent = sampleOverviewComponent;
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.sampleInformationService = sampleInformationService;
  }

  public void projectId(ProjectId projectId) {
    this.projectId = projectId;
    sampleOverviewComponent.projectId(projectId);
    displaySampleView();
  }

  private void initNoSampleRegistered() {
    Span templateHeader = new Span("No Samples Registered");
    templateHeader.addClassName("font-bold");
    Span templateText = new Span("Start your project by registering the first sample batch");
    Button registerBatchButton = new Button("Register Samples");
    registerBatchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    noSamplesInProject.add(templateHeader, templateText, registerBatchButton);
    noSamplesInProject.addClassName(AlignItems.CENTER);
    noSamplesInProject.addClassName(JustifyContent.CENTER);
    noSamplesInProject.setSizeFull();
    noSamplesInProject.setMinWidth(100, Unit.PERCENTAGE);
    noSamplesInProject.setMinHeight(100, Unit.PERCENTAGE);
  }

  private void initNoExperimentalGroupsDefined() {
    Span templateHeader = new Span("No Experimental Groups Defined");
    templateHeader.addClassName("font-bold");
    Span templateText = new Span("Start your project by registering the first experimentalGroup");
    Button registerGroupButton = new Button("Register Group");
    registerGroupButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    noExperimentalGroupInProject.add(templateHeader, templateText, registerGroupButton);
    noExperimentalGroupInProject.addClassName(AlignItems.CENTER);
    noExperimentalGroupInProject.addClassName(JustifyContent.CENTER);
    noExperimentalGroupInProject.setSizeFull();
    noExperimentalGroupInProject.setMinWidth(100, Unit.PERCENTAGE);
    noExperimentalGroupInProject.setMinHeight(100, Unit.PERCENTAGE);
  }


  private boolean areExperimentGroupsInProject(ProjectId projectId) {
    Project project = projectInformationService.find(projectId).get();
    return project.experiments().stream()
        .anyMatch(experimentInformationService::hasExperimentalGroup);
  }

  private boolean areSamplesInProject(ProjectId projectId) {
    Project project = projectInformationService.find(projectId).get();
    return project.experiments().stream()
        .anyMatch(
            experimentId -> !sampleInformationService.retrieveSamplesForExperiment(experimentId)
                .getValue().isEmpty());
  }

  private void displaySampleView() {
    if (!areExperimentGroupsInProject(projectId)) {
      displayComponentInContent(noExperimentalGroupInProject);
    } else if (!areSamplesInProject(projectId)) {
      displayComponentInContent(noSamplesInProject);
    } else {
      displayComponentInContent(sampleOverviewComponent);
    }
  }

  private void displayComponentInContent(Component component) {
    if (!isComponentInContent(component)) {
      this.removeAll();
      add(component);
    }
  }

  private boolean isComponentInContent(Component component) {
    return this.getChildren().collect(Collectors.toSet()).contains(component);
  }
}
