package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;

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

}

