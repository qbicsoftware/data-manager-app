package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEvent;
import java.io.Serial;

/**
 * <b>Disclaimer Confirmed Event</b>
 *
 * <p>Event that is fired when a user confirmed a disclaimer.</p>
 *
 * @since 1.0.0
 */
public class DisclaimerConfirmedEvent extends ComponentEvent<Disclaimer> {

  @Serial
  private static final long serialVersionUID = -7253095280300341597L;

  public DisclaimerConfirmedEvent(Disclaimer source, boolean fromClient) {
    super(source, fromClient);
  }
}
