package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import java.io.Serial;

/**
 * <p>Indicates that a user submitted an experiment creation request</p>
 *
 * @since 1.0.0
 */
public class ExperimentCreationRequestedEvent extends
    com.vaadin.flow.component.ComponentEvent<ExperimentCreationDialog> {

  @Serial
  private static final long serialVersionUID = 1009795849576160147L;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public ExperimentCreationRequestedEvent(ExperimentCreationDialog source, boolean fromClient) {
    super(source, fromClient);
  }
}
