package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Collection;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentInformationDialog;
import life.qbic.datamanager.views.support.experiment.ExperimentItem;
import life.qbic.datamanager.views.support.experiment.ExperimentItem.ExperimentItemClickedEvent;
import life.qbic.datamanager.views.support.experiment.ExperimentItemCollection;
import life.qbic.datamanager.views.support.experiment.ExperimentItemCollection.AddExperimentClickEvent;
import life.qbic.datamanager.views.support.experiment.ExperimentItemCollection.ExperimentSelectionEvent;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment list component
 * <p>
 * The list component is a {@link PageArea} component, which is responsible for showing the
 * {@link Experiment} information for all experiments in its {@link ExperimentItemCollection }
 * within the currently examined {@link life.qbic.projectmanagement.domain.project.Project}.
 * <p>
 * Additionally, it provides the possibility to create new experiments with its
 * {@link ExperimentInformationDialog} and enables the user to select an experiment of interest via
 * clicking on the {@link ExperimentItem} associated with the experiment.
 * <p>
 * Finally, it allows components to be informed about a new experiment creation or selection via the
 * {@link
 * life.qbic.datamanager.views.support.experiment.ExperimentItemCollection.ExperimentSelectionEvent}
 * event.
 */
@SpringComponent
@UIScope
public class ExperimentListComponent extends PageArea {
  @Serial
  private static final long serialVersionUID = -2196400941684042549L;
  private final ExperimentItemCollection experimentItemCollection;
  private final transient ExperimentInformationService experimentInformationService;
  private Context context;


  public ExperimentListComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired ExperimentalDesignSearchService experimentalDesignSearchService) {
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(experimentalDesignSearchService);
    this.experimentInformationService = experimentInformationService;
    this.experimentItemCollection = ExperimentItemCollection.create();
    this.add(experimentItemCollection);
    this.addClassName("experiment-list-component");
    addItemCollectionListeners();
  }

  public void setContext(Context context) {
    this.context = context;
    refresh();
  }

  private void loadExperimentsForProject(ProjectId projectId) {
    experimentItemCollection.removeAll();
    Collection<Experiment> foundExperiments = experimentInformationService.findAllForProject(
        projectId);
    if (foundExperiments.isEmpty()) {
      experimentItemCollection.showNoExperimentDisclaimer(true);
    } else {
      experimentItemCollection.showNoExperimentDisclaimer(false);
      foundExperiments.forEach(this::addExperimentToExperimentItemCollection);
    }
  }



  public void refresh() {
    loadExperimentsForProject(context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context)));
  }

  private void addItemCollectionListeners() {
    experimentItemCollection.addExperimentSelectionListener(this::fireEvent);
    experimentItemCollection.addAddButtonListener(this::fireEvent);
  }


  private void addExperimentToExperimentItemCollection(Experiment experiment) {
    experimentItemCollection.addExperimentItem(ExperimentItem.create(experiment));
  }

  public void addAddButtonListener(ComponentEventListener<AddExperimentClickEvent> listener) {
    addListener(AddExperimentClickEvent.class, listener);
  }

  /**
   * Adds the provided component listener for {@link ExperimentItemClickedEvent}
   */
  public void addExperimentSelectionListener(
      ComponentEventListener<ExperimentSelectionEvent> experimentSelectionListener) {
    addListener(ExperimentSelectionEvent.class, experimentSelectionListener);
  }

}
