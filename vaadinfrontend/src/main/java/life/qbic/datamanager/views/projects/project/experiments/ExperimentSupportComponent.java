package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Collection;
import java.util.Objects;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.ExperimentCreationListener;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.ExperimentSelectionListener;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment support component
 * <p>
 * The support component is a {@link Div} container, which is responsible for hosting the components
 * handling the sidebar content within the {@link ExperimentInformationMain}. It propagates the
 * experiment information provided in the {@link ExperimentListComponent} to the
 * {@link ExperimentInformationMain} and vice versa and can be easily extended with additional
 * components if necessary
 */
@SpringComponent
@UIScope
public class ExperimentSupportComponent extends Div {

  @Serial
  private static final long serialVersionUID = -6996282848714468102L;
  private final ExperimentListComponent experimentListComponent;
  private static final Logger log = LoggerFactory.logger(ExperimentSupportComponent.class);

  public ExperimentSupportComponent(@Autowired ExperimentListComponent experimentListComponent) {
    Objects.requireNonNull(experimentListComponent);
    this.experimentListComponent = experimentListComponent;
    layoutComponent();
  }

  private void layoutComponent() {
    this.add(experimentListComponent);
  }

  /**
   * Provides the {@link ProjectId} to the components within this container
   * <p>
   * This method serves as an entry point providing the necessary {@link ProjectId} to components
   * within this component, so they can retrieve the information associated with the
   * {@link ProjectId}
   */
  public void projectId(ProjectId projectId) {
    experimentListComponent.setProject(projectId);
  }

  /**
   * Provides the collection of {@link Experiment} to the components within this container
   * <p>
   * This method should be used to provide the experiments within a
   * {@link life.qbic.projectmanagement.domain.project.Project} to {@link ExperimentListComponent}
   */
  public void setExperiments(Collection<Experiment> experiments) {
    experimentListComponent.setExperiments(experiments);
  }

  /**
   * Provides the {@link ExperimentId} which annotates an active Experiment within the project to
   * the components within this container
   * <p>
   * This method serves as an entry point providing the {@link ExperimentId} which was set as the
   * active experiment within a {@link life.qbic.projectmanagement.domain.project.Project} to
   * components within this container.
   */
  public void setActiveExperiment(ExperimentId experimentId) {
    experimentListComponent.setActiveExperiment(experimentId);
  }

  /**
   * Provides the {@link ExperimentId} which annotates the currently selected Experiment the
   * components within this container
   * <p>
   * This method serves as an entry point providing {@link ExperimentId} which was selected by the
   * user to the components within this container.
   */
  public void setSelectedExperiment(ExperimentId experimentId) {
    experimentListComponent.setSelectedExperiment(experimentId);
  }

  /**
   * Propagates the listener which will retrieve notification if an {@link Experiment} was selected
   * to the {@link ExperimentListComponent} within this container
   */
  public void addExperimentSelectionListener(
      ExperimentSelectionListener experimentSelectionListener) {
    experimentListComponent.addExperimentSelectionListener(experimentSelectionListener);
  }

  /**
   * Propagates the listener which will retrieve notification if a new {@link Experiment} was
   * created in the {@link ExperimentListComponent} within this container
   */
  public void addExperimentCreationListener(ExperimentCreationListener experimentCreationListener) {
    experimentListComponent.addExperimentCreationListener(experimentCreationListener);
  }

}
