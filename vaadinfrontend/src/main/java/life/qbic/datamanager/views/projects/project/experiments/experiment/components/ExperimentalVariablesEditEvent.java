package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Experimental Variables Event</b>
 *
 * <p>Indicates that a user wants to edit experimental variables information
 * for a given experiment</p>
 *
 * @since 1.0.0
 */
public class ExperimentalVariablesEditEvent extends ComponentEvent<ExperimentalVariablesComponent> {

  @Serial
  private static final long serialVersionUID = -7777255533105234741L;

  public ExperimentalVariablesEditEvent(ExperimentalVariablesComponent source, boolean fromClient) {
    super(source, fromClient);
  }
}
