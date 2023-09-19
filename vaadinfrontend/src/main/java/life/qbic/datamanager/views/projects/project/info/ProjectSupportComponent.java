package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.AddExperimentClickEvent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.ExperimentSelectionEvent;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project support component
 * <p>
 * The support component is a {@link Div} container, which is responsible for hosting the components
 * handling the sidebar content within the {@link ProjectInformationMain}. It propagates the project
 * information provided in the {@link ProjectLinksComponent} and the experiment information provided
 * in the {@link ExperimentListComponent} to the {@link ProjectInformationMain}, and vice
 * versa and can be easily extended with additional components if necessary
 */
@SpringComponent
@UIScope
public class ProjectSupportComponent extends Div {

  @Serial
  private static final long serialVersionUID = -6996282848714468102L;
  private final ProjectLinksComponent projectLinksComponent;
  private final ExperimentListComponent experimentListComponent;
  private static final Logger log = LoggerFactory.logger(ProjectSupportComponent.class);


  public ProjectSupportComponent(@Autowired ProjectLinksComponent projectLinksComponent,
      @Autowired ExperimentListComponent experimentListComponent) {
    Objects.requireNonNull(projectLinksComponent);
    Objects.requireNonNull(experimentListComponent);
    this.experimentListComponent = experimentListComponent;
    this.projectLinksComponent = projectLinksComponent;
    layoutComponent();
    addListeners();
  }

  private void layoutComponent() {
    this.add(experimentListComponent);
    this.add(projectLinksComponent);
  }

  private void addListeners() {
    experimentListComponent.addExperimentSelectionListener(this::fireEvent);
    experimentListComponent.addAddButtonListener(this::fireEvent);
  }

  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    projectLinksComponent.setContext(context);
    experimentListComponent.setContext(context);
  }

  /**
   * Propagates the listener which will retrieve notification if an {@link Experiment} was selected
   * to the {@link ExperimentListComponent} within this container
   */
  public void addExperimentSelectionListener(
      ComponentEventListener<ExperimentSelectionEvent> listener) {
    addListener(ExperimentSelectionEvent.class, listener);
  }

  public void addExperimentAddButtonClickEventListener(
      ComponentEventListener<AddExperimentClickEvent> listener) {
    addListener(AddExperimentClickEvent.class, listener);
  }

}
