package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentInformationDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentCreatedEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentInformationContent;
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
 * Experiment list component
 * <p>
 * The list component is a {@link PageArea} component, which is responsible for showing the
 * {@link Experiment} information for all experiments in its {@link ExperimentItemCollection }
 * within the currently examined {@link life.qbic.projectmanagement.domain.project.Project}.
 * Additionally, it provides the possibility to create new experiments with its
 * {@link ExperimentInformationDialog} and enables the user to select an experiment of interest via
 * clicking on the {@link ExperimentItem} associated with the experiment. Finally, it allows
 * components to be informed about a new experiment creation or selection via the
 * {@link ExperimentCreationListener} and {@link ExperimentSelectionListener} provided in this
 * component.
 */
@SpringComponent
@UIScope
public class ExperimentListComponent extends PageArea {
  @Serial
  private static final long serialVersionUID = -2196400941684042549L;
  private final ExperimentItemCollection experimentItemCollection;
  private final List<ExperimentSelectionListener> selectionListeners = new ArrayList<>();
  private final List<ExperimentCreationListener> creationListener = new ArrayList<>();
  private final transient ExperimentInformationService experimentInformationService;
  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient ExperimentalDesignSearchService experimentalDesignSearchService;
  private Context context;

  public ExperimentListComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired ExperimentalDesignSearchService experimentalDesignSearchService) {
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(addExperimentToProjectService);
    Objects.requireNonNull(experimentalDesignSearchService);
    this.addExperimentToProjectService = addExperimentToProjectService;
    this.experimentInformationService = experimentInformationService;
    this.experimentalDesignSearchService = experimentalDesignSearchService;
    this.addClassName("experiment-list-component");
    this.experimentItemCollection = ExperimentItemCollection.create();
    this.add(experimentItemCollection);
    addItemCollectionListeners();
  }

  public void setContext(Context context) {
    ProjectId projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context));
    context.experimentId()
        .orElseThrow(() -> new ApplicationException("no experiment id in context " + context));
    this.context = context;
    loadExperimentsForProject(projectId);
  }

  private void loadExperimentsForProject(ProjectId projectId) {
    experimentItemCollection.removeAll();
    Collection<Experiment> foundExperiments = experimentInformationService.findAllForProject(
        projectId);
    foundExperiments.forEach(this::addExperimentToExperimentItemCollection);
  }

  private void addItemCollectionListeners() {
    experimentItemCollection.addClickEventListener(this::fireExperimentalItemSelectedEvent);
    experimentItemCollection.addAddEventListener(event -> generateExperimentInformationDialog());
  }

  private void generateExperimentInformationDialog() {
    var creationDialog = new ExperimentInformationDialog(experimentalDesignSearchService);
    creationDialog.addCancelEventListener(
        experimentInformationDialogCancelEvent -> creationDialog.close());
    creationDialog.addConfirmEventListener(experimentInformationDialogConfirmEvent -> {
      ProjectId projectId = context.projectId().orElseThrow();
      ExperimentId createdExperiment = createExperiment(projectId,
          experimentInformationDialogConfirmEvent.getSource()
              .content());
      setSelectedExperiment(createdExperiment);
      creationDialog.close();
      fireExperimentCreatedEvent(
          new ExperimentCreatedEvent(creationDialog, createdExperiment, true));
      displayExperimentCreationSuccess();
    });
    creationDialog.open();
  }

  private ExperimentId createExperiment(ProjectId projectId,
      ExperimentInformationContent experimentInformationContent) {
    Result<ExperimentId, RuntimeException> result = addExperimentToProjectService.addExperimentToProject(
        projectId,
        experimentInformationContent.experimentName(), experimentInformationContent.species(),
        experimentInformationContent.specimen(), experimentInformationContent.analytes());
    if (result.isValue()) {
      return result.getValue();
    } else {
      throw new ApplicationException("Experiment Creation failed");
    }
  }

  private void addExperimentToExperimentItemCollection(Experiment experiment) {
    experimentItemCollection.addExperimentItem(ExperimentItem.create(experiment));
  }

  /**
   * Provides the {@link ExperimentId} which annotates the currently active Experiment to this
   * component
   * <p>
   * This informs the {@link ExperimentItemCollection} about which experiment is set as active via
   * the provided {@link ExperimentId}
   */
  public void setActiveExperiment(ExperimentId experimentId) {
    experimentItemCollection.findBy(experimentId).ifPresent(ExperimentItem::setAsActive);
  }

  /**
   * Provides the {@link ExperimentId} which annotates the currently selected Experiment to this
   * component
   * <p>
   * This informs the {@link ExperimentItemCollection} about which experiment was selected by the
   * user via the provided {@link ExperimentId}
   */
  public void setSelectedExperiment(ExperimentId experimentId) {
    experimentItemCollection.findBy(experimentId).ifPresent(ExperimentItem::setAsSelected);
  }

  /**
   * Adds the provided {@link ExperimentSelectionListener} to the list of listeners which will
   * retrieve notification if an {@link Experiment} was selected within this component
   */
  public void addExperimentSelectionListener(
      ExperimentSelectionListener experimentSelectionListener) {
    this.selectionListeners.add(experimentSelectionListener);
  }

  /**
   * Adds the provided {@link ExperimentCreationListener} to the list of listeners which will
   * retrieve notification if a new {@link Experiment} was created in this component
   */
  public void addExperimentCreationListener(ExperimentCreationListener experimentCreationListener) {
    this.creationListener.add(experimentCreationListener);
  }

  private void fireExperimentCreatedEvent(ExperimentCreatedEvent event) {
    creationListener.forEach(it -> it.handle(event));
  }

  private void fireExperimentalItemSelectedEvent(ExperimentItemClickedEvent event) {
    selectionListeners.forEach(it -> it.handle(event));
  }

  private void displayExperimentCreationSuccess() {
    SuccessMessage successMessage = new SuccessMessage("Experiment Creation succeeded", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  /**
   * Experiment Selection Interface
   * <p>
   * Represents a simple interface to handle {@link ExperimentItemClickedEvent} that can be invoked
   * by the method {@link ExperimentSelectionListener#handle(ExperimentItemClickedEvent)}.
   * <p>
   * This interface is suitable for all components that want to be informed if an
   * {@link ExperimentItemClickedEvent} occurred, and want to handle the information stored in the
   * {@link ExperimentItemClickedEvent}
   */
  @FunctionalInterface
  public interface ExperimentSelectionListener {

    void handle(ExperimentItemClickedEvent event);
  }

  /**
   * Experiment Creation Interface
   * <p>
   * Represents a simple interface to handle {@link ExperimentCreatedEvent} that can be invoked by
   * the method {@link ExperimentCreationListener#handle(ExperimentCreatedEvent)}.
   * <p>
   * This interface is suitable for all components that want to be informed if an
   * {@link ExperimentCreatedEvent} occurred, and want to handle the information stored in the
   * {@link ExperimentCreatedEvent}
   */
  @FunctionalInterface
  public interface ExperimentCreationListener {

    void handle(ExperimentCreatedEvent event);
  }
}
