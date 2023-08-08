package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * <b>Experiment Edit Event</b>
 * <p>
 * Event that indicates that the user wants to edit an experiment via the
 * {@link ExperimentDetailsComponent}
 *
 * @since 1.0.0
 */
public class ExperimentEditEvent extends ComponentEvent<ExperimentDetailsComponent> {

  @Serial
  private static final long serialVersionUID = -5383275108609304372L;
  private final ExperimentId experimentId;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source       the source component
   * @param experimentId the {@link ExperimentId} of the edited experiment
   * @param fromClient   <code>true</code> if the event originated from the client
   *                     side, <code>false</code> otherwise
   */
  public ExperimentEditEvent(ExperimentDetailsComponent source, ExperimentId experimentId,
      boolean fromClient) {
    super(source, fromClient);
    this.experimentId = experimentId;
  }

  public ExperimentId experimentId() {
    return experimentId;
  }
}
