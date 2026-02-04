package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.shared.Registration;

public interface ItemCountChangePublisher {

  /**
   * Registers a listener that is notified when the number of items changes.
   *
   * <p>
   * This allows clients to react to changes caused by filtering, paging, or backend data updates
   * without accessing Vaadin-specific APIs.
   * </p>
   *
   * @param listener the listener to register
   * @return a registration handle used to remove the listener
   * @see com.vaadin.flow.data.provider.DataView#addItemCountChangeListener(ComponentEventListener)
   */
  Registration addItemCountChangeListener(
      ComponentEventListener<ItemCountChangeEvent<?>> listener);
}
