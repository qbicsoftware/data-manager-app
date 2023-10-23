package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Creation Clicked Event</b>
 * <p>
 * Event that indicates that a creation card has been clicked by the user.
 *
 * @since 1.0.0
 */
public class CreationClickedEvent extends ComponentEvent<CreationCard> {

  @Serial
  private static final long serialVersionUID = -7576323024359012924L;

  public CreationClickedEvent(CreationCard source, boolean fromClient) {
    super(source, fromClient);
  }
}
