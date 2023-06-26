package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Experimental Group Deletion Event</b>
 * <p>
 * Event that indicates that the user wants to delete a certain experimental group via the
 * {@link ExperimentalGroupCard}
 *
 * @since 1.0.0
 */
public class ExperimentalGroupDeletionEvent extends ComponentEvent<ExperimentalGroupCard> {

  @Serial
  private static final long serialVersionUID = -638574743420859609L;

  public ExperimentalGroupDeletionEvent(ExperimentalGroupCard source, boolean fromClient) {
    super(source, fromClient);
  }
}
