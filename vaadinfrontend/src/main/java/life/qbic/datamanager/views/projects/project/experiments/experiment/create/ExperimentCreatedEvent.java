package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * <b>Experiment Creation Event</b>
 *
 * <p>Indicates that an experiment was added.</p>
 *
 * @since 1.0.0
 */
public class ExperimentCreatedEvent extends ComponentEvent<ExperimentCreationDialog> {

  @Serial
  private static final long serialVersionUID = 7876350076650569558L;
  private final ExperimentId experimentId;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source       the source component
   * @param experimentId
   * @param fromClient   <code>true</code> if the event originated from the client
   *                     side, <code>false</code> otherwise
   */
  public ExperimentCreatedEvent(ExperimentCreationDialog source, ExperimentId experimentId,
      boolean fromClient) {
    super(source, fromClient);
    this.experimentId = experimentId;
  }

  public ExperimentId experimentId() {
    return experimentId;
  }
}
