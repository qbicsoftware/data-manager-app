package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import life.qbic.datamanager.views.general.DomainSelectionComponent.DomainSelectionEvent;

/**
 * <b>Domain Selection Component</b>
 * <p>
 * A domain selection component can be used to expose user selection events for a certain domain
 * within a component.
 * <p>
 * E.g., a user wants to perform a certain action in the scope of a known domain like genomics or
 * proteomics, than whenever the user changes the selected domain, a new
 * {@link DomainSelectionEvent} is fired.
 *
 * @since 1.11.0
 */
public interface DomainSelectionComponent<T extends Component, E extends DomainSelectionEvent<T>> {

  /**
   * Adds a {@link ComponentEventListener<E>} to the component's subscription to be informed on
   * {@link DomainSelectionEvent}.
   *
   * @param listener the listener to be informed on {@link DomainSelectionEvent}.
   * @return a {@link Registration} to confirm the subscription.
   * @since 1.11.0
   */
  Registration addDomainSelectionListener(ComponentEventListener<E> listener);

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
   class DomainSelectionEvent<T extends Component> extends ComponentEvent<T> {

    public DomainSelectionEvent(T source, boolean fromClient) {
      super(source, fromClient);
    }

  }
}
