package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.shared.Registration;

public interface GridFilterConfiguration<T, F> {

  void setFilter(F filter);

  Registration addItemCountChangeListener(
      ComponentEventListener<ItemCountChangeEvent<?>> listener);

  Grid<T> getGrid();

}
