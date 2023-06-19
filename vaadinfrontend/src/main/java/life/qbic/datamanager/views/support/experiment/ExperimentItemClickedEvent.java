package life.qbic.datamanager.views.support.experiment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentItemClickedEvent extends ClickEvent<ExperimentItem> {

  @Serial
  private static final long serialVersionUID = -731168000425844756L;

  public ExperimentItemClickedEvent(Component source, boolean fromClient) {
    super(source);
  }

}
