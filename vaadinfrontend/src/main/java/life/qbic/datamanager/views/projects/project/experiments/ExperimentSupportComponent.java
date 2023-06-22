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
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@SpringComponent
@UIScope
public class ExperimentSupportComponent extends Div {

  @Serial
  private static final long serialVersionUID = -6996282848714468102L;
  private ProjectId projectId;
  private final ExperimentListComponent experimentListComponent;
  private static final Logger log = LoggerFactory.logger(ExperimentSupportComponent.class);

  public ExperimentSupportComponent(@Autowired ExperimentListComponent experimentListComponent) {
    Objects.requireNonNull(experimentListComponent);
    this.experimentListComponent = experimentListComponent;
    this.add(experimentListComponent);
    this.addClassName("support");
  }

  public void setProjectId(ProjectId projectId) {
    this.projectId = projectId;
    experimentListComponent.setProject(projectId);
  }

  public void setExperiments(Collection<Experiment> experiments) {
    experimentListComponent.setExperiments(experiments);
  }

  public void setActiveExperiment(ExperimentId experimentId) {
    experimentListComponent.setActiveExperiment(experimentId);
  }

  public void setSelectedExperiment(ExperimentId experimentId) {
    experimentListComponent.setSelectedExperiment(experimentId);
  }

  public void addActiveExperimentSelectionListener(
      ExperimentSelectionListener experimentSelectionListener) {
    experimentListComponent.addExperimentSelectionListener(experimentSelectionListener);
  }

  public void addExperimentCreationListener(ExperimentCreationListener experimentCreationListener) {
    experimentListComponent.addExperimentCreationListener(experimentCreationListener);
  }

}
