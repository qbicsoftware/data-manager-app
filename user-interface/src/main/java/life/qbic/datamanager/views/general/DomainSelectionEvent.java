package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DomainSelectionEvent<T extends Component> extends ComponentEvent<T> {

  public DomainSelectionEvent(T source, boolean fromClient) {
    super(source, fromClient);
  }

}


