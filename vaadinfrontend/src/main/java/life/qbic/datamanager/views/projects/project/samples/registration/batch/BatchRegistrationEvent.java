package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Batch Registration Event</b>
 *
 * <p>Indicates that a user submitted a batch registration request</p>
 *
 * @since 1.0.0
 */
public class BatchRegistrationEvent extends ComponentEvent<BatchRegistrationDialog> {

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
  public BatchRegistrationEvent(BatchRegistrationDialog source, boolean fromClient) {
    super(source, fromClient);
  }

}
