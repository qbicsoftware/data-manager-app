package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Confirm Event</b>
 *
 * <p>Event that indicates, that a user wants to confirm the current action of a component
 * {@link T}</p>.
 *
 * @since 1.0.0
 */
public class ConfirmEvent<T extends Component> extends ComponentEvent<T> {

  @Serial
  private static final long serialVersionUID = 5052106948523580116L;

  public ConfirmEvent(T source, boolean fromClient) {
    super(source, fromClient);
  }
}
