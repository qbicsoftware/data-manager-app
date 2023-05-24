package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class CreationClickedEvent extends ComponentEvent<CreationCard> {

  @Serial
  private static final long serialVersionUID = -7576323024359012924L;

  public CreationClickedEvent(CreationCard source, boolean fromClient) {
    super(source, fromClient);
  }
}
