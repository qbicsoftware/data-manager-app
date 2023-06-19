package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Add new experimental variable event</b>
 * <p>
 * This event indicates, that the user wants to add one ore more new experimental variables to the
 * experiment.
 *
 * @since 1.0.0
 */
public class AddNewExperimentalVariableEvent extends
    ComponentEvent<ExperimentalVariablesComponent> {

  @Serial
  private static final long serialVersionUID = 4816480598791695369L;

  public AddNewExperimentalVariableEvent(ExperimentalVariablesComponent source,
      boolean fromClient) {
    super(source, fromClient);
  }
}
