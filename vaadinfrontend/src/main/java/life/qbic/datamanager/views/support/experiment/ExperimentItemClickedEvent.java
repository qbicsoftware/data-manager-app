package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import java.io.Serial;

/**
 * <b>Experiment Item Clicked Event</b>
 * <p>
 * An event that indicates that an experiment item has been clicked by a user.
 *
 * @since 1.0.0
 */
public class ExperimentItemClickedEvent extends ClickEvent<ExperimentItem> {

  @Serial
  private static final long serialVersionUID = -731168000425844756L;

  public ExperimentItemClickedEvent(Component source, boolean fromClient) {
    super(source);
  }

}
