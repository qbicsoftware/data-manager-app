package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Experiment Creation Event</b>
 *
 * <p>Indicates that a user submitted an experiment creation request</p>
 *
 * @since 1.0.0
 */
public class ExperimentCreationEvent extends ComponentEvent<ExperimentCreationDialog> {

  @Serial
  private static final long serialVersionUID = 7876350076650569558L;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public ExperimentCreationEvent(ExperimentCreationDialog source, boolean fromClient) {
    super(source, fromClient);
  }
}
