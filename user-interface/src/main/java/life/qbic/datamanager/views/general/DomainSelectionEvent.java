package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Domain Selection Event
 * <p>
 * Event type that can be used to indicate that the user intended to change the domain scope for a
 * task.
 * <p>
 * Sometimes actions are very similar but not exactly the same, because they depend on the
 * measurement domain. E.g., metadata validation needs to be different for proteomics measurements
 * than for next generation sequencing measurements.
 * <p>
 * In case the application offers the user with multiple selection options for a domain, this event
 * can be used to indicate a change in the selection.
 *
 * @since 1.11.0
 */
public class DomainSelectionEvent<T extends Component> extends ComponentEvent<T> {

  public DomainSelectionEvent(T source, boolean fromClient) {
    super(source, fromClient);
  }

}


