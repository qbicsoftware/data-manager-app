package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupDeletionEvent extends ComponentEvent<ExperimentalGroupCard> {

  @Serial
  private static final long serialVersionUID = -638574743420859609L;

  public ExperimentalGroupDeletionEvent(ExperimentalGroupCard source, boolean fromClient) {
    super(source, fromClient);
  }
}
