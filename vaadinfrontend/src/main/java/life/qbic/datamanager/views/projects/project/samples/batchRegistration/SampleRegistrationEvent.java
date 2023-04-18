package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Sample Registration Event</b>
 *
 * <p>Indicates that a user submitted a sample registration request</p>
 *
 * @since 1.0.0
 */
public class SampleRegistrationEvent extends ComponentEvent<SampleRegistrationDialog> {

  @Serial
  private static final long serialVersionUID = 9182977233447104965L;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public SampleRegistrationEvent(SampleRegistrationDialog source, boolean fromClient) {
    super(source, fromClient);
  }

}
