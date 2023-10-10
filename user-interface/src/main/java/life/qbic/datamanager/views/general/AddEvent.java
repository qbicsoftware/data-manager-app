package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Add event</b>
 * <p>
 * Indicates that a user wants to add information to a component
 *
 * @since 1.0.0
 */
public class AddEvent<T extends Component> extends ComponentEvent<T> {

  @Serial
  private static final long serialVersionUID = 9114334039868158765L;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public AddEvent(T source, boolean fromClient) {
    super(source, fromClient);
  }
}
