package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;
import life.qbic.datamanager.views.projects.project.samples.BatchDetailsComponent;

/**
 * <b>Batch Deletion Event</b>
 *
 * <p>Indicates that a user submitted a batch deletion request</p>
 *
 * @since 1.0.0
 */

public class BatchDeletionEvent extends ComponentEvent<BatchDetailsComponent> {

  @Serial
  private static final long serialVersionUID = -2975369166457982204L;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public BatchDeletionEvent(BatchDetailsComponent source, boolean fromClient) {
    super(source, fromClient);
  }

}
