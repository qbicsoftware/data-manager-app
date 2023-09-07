package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.support.experiment.ExperimentItem.ExperimentItemClickedEvent;
import life.qbic.datamanager.views.support.experiment.ExperimentItemCollection.AddExperimentClickEvent;
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

  public ExperimentSupportComponent(@Autowired ExperimentListComponent experimentListComponent) {
    Objects.requireNonNull(experimentListComponent);
    this.experimentListComponent = experimentListComponent;
    experimentListComponent.addAddButtonListener(this::fireEvent); //propagate event
    experimentListComponent.addExperimentSelectionListener(this::fireEvent);
    layoutComponent();
  }

  private void layoutComponent() {
    this.add(experimentListComponent);
  }

  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    experimentListComponent.setContext(context);
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
      ComponentEventListener<ExperimentItemClickedEvent> listener) {
    this.addListener(ExperimentItemClickedEvent.class, listener);
  }

  public void addExperimentAddButtonClickEventListener(
      ComponentEventListener<AddExperimentClickEvent> listener) {
    addListener(AddExperimentClickEvent.class, listener);
  }
}
