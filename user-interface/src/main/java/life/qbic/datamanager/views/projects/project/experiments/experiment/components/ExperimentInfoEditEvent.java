package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Experiment Info Edit Event</b>
 *
 * <p>Indicates that the user wants to edit experiment information</p>
 *
 * @since 1.0.0
 */
public class ExperimentInfoEditEvent extends ComponentEvent<ExperimentInfoComponent> {

  @Serial
  private static final long serialVersionUID = 6769346120310773891L;

  public ExperimentInfoEditEvent(ExperimentInfoComponent source, boolean fromClient) {
    super(source, fromClient);
  }

}
