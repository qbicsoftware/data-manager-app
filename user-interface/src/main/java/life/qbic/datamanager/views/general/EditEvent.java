package life.qbic.datamanager.views.general;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Edit Event</b>
 *
 * <p>Indicates that a user wants to edit information in a component.</p>
 *
 * @since 1.0.0
 */
public class EditEvent<T extends Component> extends ComponentEvent<T> {

  @Serial
  private static final long serialVersionUID = -1033061739347133861L;

  /**
   * Creates a new event using the given source and indicator whether the event originated from the
   * client side or the server side.
   *
   * @param source     the source component
   * @param fromClient <code>true</code> if the event originated from the client
   *                   side, <code>false</code> otherwise
   */
  public EditEvent(T source, boolean fromClient) {
    super(source, fromClient);
  }
}
