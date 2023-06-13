package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class CancelEvent<T extends Component> extends ComponentEvent<T> {

  @Serial
  private static final long serialVersionUID = -6296065942754554336L;

  public CancelEvent(T source, boolean fromClient) {
    super(source, fromClient);
  }
}
