package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationContent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationContent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.SampleInformationService;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.application.batch.BatchInformationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService.ResponseCode;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
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
  private final Span titleSpan = new Span("Samples");
  private final Div content = new Div();
  private final BatchRegistrationDialog batchRegistrationDialog = new BatchRegistrationDialog();
  private final SampleOverviewComponent sampleOverviewComponent;
  private final Div noExperimentalGroupsDefinedInProject = new Div();
  private final Div noSamplesRegisteredInProject = new Div();
  private ProjectId projectId;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient SampleInformationService sampleInformationService;
  private final transient BatchRegistrationService batchRegistrationService;
  private final transient SampleRegistrationService sampleRegistrationService;

  public SampleInformationContent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired BatchInformationService batchInformationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired SampleRegistrationService sampleRegistrationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(batchInformationService);
    Objects.requireNonNull(sampleInformationService);
    Objects.requireNonNull(batchRegistrationService);
    Objects.requireNonNull(sampleRegistrationService);
    initContentLayout();
    sampleOverviewComponent = new SampleOverviewComponent(batchInformationService,
        sampleInformationService);
    initNoSampleRegistered();
    initNoExperimentalGroupsDefined();
    addEventListeners();
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.sampleInformationService = sampleInformationService;
    this.batchRegistrationService = batchRegistrationService;
    this.sampleRegistrationService = sampleRegistrationService;
  }

  private void initContentLayout() {
    this.addClassName("sample-content-area");
    content.addClassName("content");
    titleSpan.addClassName("title");
    this.add(titleSpan);
    this.add(content);
  }

  private void initNoSampleRegistered() {
    Span noSamplesRegisteredTitle = new Span("No samples registered");
    noSamplesRegisteredTitle.addClassName("title");
    Paragraph noSamplesRegisteredDescription = new Paragraph(
        "Start your project by registering the first sample batch");
    noSamplesRegisteredDescription.addClassName("description");
    Button registerSamples = new Button("Register batch");
    registerSamples.addClickListener(this::openBatchRegistrationDialog);
    registerSamples.addClassName("primary");
    noSamplesRegisteredInProject.add(noSamplesRegisteredTitle, noSamplesRegisteredDescription,
        registerSamples);
    noSamplesRegisteredInProject.addClassName("no-samples-defined");
  }

  private void initNoExperimentalGroupsDefined() {
    Span noExperimentalGroupsTitle = new Span("No experimental groups defined");
    noExperimentalGroupsTitle.addClassName("title");
    Paragraph noExperimentalGroupsDescription = new Paragraph(
        "Start your project by registering the first experimental group");
    noExperimentalGroupsDescription.addClassName("description");
    Button addExperimentalGroup = new Button("Add Experimental Group");
    addExperimentalGroup.addClickListener(this::routeToExperimentalGroupCreation);
    addExperimentalGroup.addClassName("primary");
    noExperimentalGroupsDefinedInProject.add(noExperimentalGroupsTitle,
        noExperimentalGroupsDescription, addExperimentalGroup);
    noExperimentalGroupsDefinedInProject.addClassName("no-groups-defined");
  }

  public void projectId(ProjectId projectId) {
    this.projectId = projectId;
    sampleOverviewComponent.setProjectId(projectId);
    projectInformationService.find(projectId).ifPresent(this::loadExperimentInformation);
    displaySampleView();
  }

  public void loadExperimentInformation(Project project) {
    List<Experiment> foundExperiments = new ArrayList<>();
    project.experiments().forEach(experimentId -> experimentInformationService.find(experimentId)
        .ifPresent(foundExperiments::add));
    if (!foundExperiments.isEmpty()) {
      sampleOverviewComponent.setExperiments(foundExperiments);
    } else {
      displayNoExperimentsFound();
    }
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
      displayComponentInContent(noExperimentalGroupsDefinedInProject);
    } else if (!areSamplesInProject(projectId)) {
      displayComponentInContent(noSamplesRegisteredInProject);
    } else {
      displayComponentInContent(sampleOverviewComponent);
    }
  }

  private void displayComponentInContent(Component component) {
    if (!isComponentInContent(component)) {
      content.removeAll();
      content.add(component);
    }
  }

  private boolean isComponentInContent(Component component) {
    return content.getChildren().collect(Collectors.toSet()).contains(component);
  }

  private void routeToExperimentalGroupCreation(ComponentEvent<?> componentEvent) {
    if (componentEvent.isFromClient()) {
      log.debug(String.format("Rerouting to experiment page of project %s", projectId.value()));
      String routeToExperimentPage = String.format(Projects.EXPERIMENTS, projectId.value());
      componentEvent.getSource().getUI().ifPresent(ui ->
          ui.navigate(routeToExperimentPage));
    }
  }

  private void displayNoExperimentsFound() {
    ErrorMessage errorMessage = new ErrorMessage("No Experiments defined",
        "No Experiments found in project");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }


  private void openBatchRegistrationDialog(ComponentEvent<?> componentEvent) {
    if (componentEvent.isFromClient()) {
      batchRegistrationDialog.open();
    }
  }

  private void addEventListeners() {
    sampleOverviewComponent.registerButton.addClickListener(this::openBatchRegistrationDialog);
    batchRegistrationDialog.addBatchRegistrationEventListener(batchRegistrationEvent -> {
      BatchRegistrationDialog batchRegistrationSource = batchRegistrationEvent.getSource();
      registerBatchAndSamples(batchRegistrationSource.batchRegistrationContent(),
          batchRegistrationSource.sampleRegistrationContent()).onValue(batchId -> {
        batchRegistrationDialog.resetAndClose();
        displayRegistrationSuccess();
      });
    });
    batchRegistrationDialog.addCancelEventListener(
        event -> batchRegistrationDialog.resetAndClose());
  }

  private Result<?, ?> registerBatchAndSamples(BatchRegistrationContent batchRegistrationContent,
      List<SampleRegistrationContent> sampleRegistrationContent) {
    return registerBatchInformation(batchRegistrationContent).onValue(
        batchId -> {
          List<SampleRegistrationRequest> sampleRegistrationsRequests = createSampleRegistrationRequests(
              batchId, batchRegistrationContent.experimentId(), sampleRegistrationContent);
          registerSamples(sampleRegistrationsRequests);
        });
  }

  private List<SampleRegistrationRequest> createSampleRegistrationRequests(BatchId batchId,
      ExperimentId experimentId,
      List<SampleRegistrationContent> sampleRegistrationContents) {
    return sampleRegistrationContents.stream()
        .map(sampleRegistrationContent -> {
          Analyte analyte = new Analyte(sampleRegistrationContent.analyte());
          Specimen specimen = new Specimen(sampleRegistrationContent.specimen());
          Species species = new Species(sampleRegistrationContent.species());
          SampleOrigin sampleOrigin = SampleOrigin.create(species, specimen, analyte);
          return new SampleRegistrationRequest(sampleRegistrationContent.label(), batchId,
              experimentId,
              sampleRegistrationContent.experimentalGroupId(),
              sampleRegistrationContent.biologicalReplicateId(), sampleOrigin);
        }).toList();
  }

  private Result<BatchId, ResponseCode> registerBatchInformation(
      BatchRegistrationContent batchRegistrationContent) {
    return batchRegistrationService.registerBatch(batchRegistrationContent.batchLabel(),
        batchRegistrationContent.isPilot()).onError(responseCode -> displayRegistrationFailure());
  }

  private void registerSamples(List<SampleRegistrationRequest> sampleRegistrationRequests) {
    sampleRegistrationService.registerSamples(sampleRegistrationRequests, projectId)
        .onError(responseCode -> displayRegistrationFailure());
  }

  private void displayRegistrationSuccess() {
    SuccessMessage successMessage = new SuccessMessage("Batch registration succeeded.", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  private void displayRegistrationFailure() {
    ErrorMessage errorMessage = new ErrorMessage("Batch registration failed.", "");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }
}
