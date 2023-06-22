package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentCreationContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentCreationDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentCreationEvent;
import life.qbic.datamanager.views.support.experiment.ExperimentItem;
import life.qbic.datamanager.views.support.experiment.ExperimentItemClickedEvent;
import life.qbic.datamanager.views.support.experiment.ExperimentItemCollection;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
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
public class ExperimentListComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = -2196400941684042549L;
  private final ExperimentItemCollection experimentItemCollection;
  private ProjectId projectId;
  private final List<ExperimentSelectionListener> selectionListeners = new ArrayList<>();
  private final List<ExperimentCreationListener> creationListener = new ArrayList<>();
  private final ExperimentCreationDialog experimentCreationDialog;
  private final transient AddExperimentToProjectService addExperimentToProjectService;

  public ExperimentListComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired ExperimentalDesignSearchService experimentalDesignSearchService) {
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(addExperimentToProjectService);
    Objects.requireNonNull(experimentalDesignSearchService);
    this.addExperimentToProjectService = addExperimentToProjectService;
    //ToDo Extract service
    this.experimentCreationDialog = new ExperimentCreationDialog(experimentalDesignSearchService);
    this.addClassName("list-component");
    this.experimentItemCollection = ExperimentItemCollection.create(
        "Add a new experiment");
    this.add(experimentItemCollection);
    addListeners();
  }

  private void addListeners() {
    addItemCollectionListeners();
    addExperimentCreationDialogListener();
  }

  private void addItemCollectionListeners() {
    experimentItemCollection.addClickEventListener(this::fireExperimentalItemSelectedEvent);
    experimentItemCollection.addCreateEventListener(event -> experimentCreationDialog.open());
  }

  private void addExperimentCreationDialogListener() {
    experimentCreationDialog.addExperimentCreationEventListener(
        event -> addExperimentToProject(event, event.getSource().content()));
    experimentCreationDialog.addCancelEventListener(
        event -> experimentCreationDialog.resetAndClose());
  }

  private void addExperimentToProject(ExperimentCreationEvent experimentCreationEvent,
      ExperimentCreationContent experimentCreationContent) {
    addExperimentToProjectService.addExperimentToProject(projectId,
            experimentCreationContent.experimentName(), experimentCreationContent.species(),
            experimentCreationContent.specimen(), experimentCreationContent.analytes())
        .onValue(experimentId -> {
          displaySuccessfulExperimentCreation();
          fireExperimentCreatedEvent(experimentCreationEvent);
          experimentCreationDialog.resetAndClose();
          setSelectedExperiment(experimentId);
        }).onError(error -> displayExperimentCreationFailure());
  }

  public void setProject(ProjectId projectId) {
    this.projectId = projectId;
  }

  public void setExperiments(Collection<Experiment> experiments) {
    experimentItemCollection.removeAll();
    addItemCollectionListeners();
    experiments.forEach(experiment -> experimentItemCollection.addExperimentItem(
        ExperimentItem.create(experiment)));
  }

  public void setActiveExperiment(ExperimentId experimentId) {
    experimentItemCollection.findBy(experimentId).ifPresent(ExperimentItem::setAsActive);
  }

  public void setSelectedExperiment(ExperimentId experimentId) {
    experimentItemCollection.findBy(experimentId).ifPresent(ExperimentItem::setAsSelected);
  }

  public void addExperimentSelectionListener(
      ExperimentSelectionListener experimentSelectionListener) {
    this.selectionListeners.add(experimentSelectionListener);
  }

  public void addExperimentCreationListener(ExperimentCreationListener experimentCreationListener) {
    this.creationListener.add(experimentCreationListener);
  }

  private void fireExperimentCreatedEvent(ExperimentCreationEvent event) {
    creationListener.forEach(it -> it.handle(event));
  }

  private void fireExperimentalItemSelectedEvent(ExperimentItemClickedEvent event) {
    selectionListeners.forEach(it -> it.handle(event));
  }

  private void displaySuccessfulExperimentCreation() {
    SuccessMessage successMessage = new SuccessMessage("Experiment Creation succeeded", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  private void displayExperimentCreationFailure() {
    ErrorMessage errorMessage = new ErrorMessage("Experiment Creation failed", "");
    StyledNotification notification = new StyledNotification(errorMessage);
    notification.open();
  }

  @FunctionalInterface
  public interface ExperimentSelectionListener {

    void handle(ExperimentItemClickedEvent event);
  }

  @FunctionalInterface
  public interface ExperimentCreationListener {

    void handle(ExperimentCreationEvent event);
  }
}
