package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.shared.Registration;

/**
 * <class short description>
 *
 * @since <version tag>
 */
public interface ItemCountObservable {

  Registration addItemCountChangedListener( ComponentEventListener<ItemCountChangeEvent<?>> listener);

}
