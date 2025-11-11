package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Cancel Event</b>
 *
 * <p>Event that indicates that the user wants to cancel the current action
 * associated with a component of type {@link T}</p>.
 *
 * @since 1.0.0
 */
public class CancelEvent<T extends Component> extends ComponentEvent<T> {

  @Serial
  private static final long serialVersionUID = -6296065942754554336L;

  public CancelEvent(T source, boolean fromClient) {
    super(source, fromClient);
  }
}
