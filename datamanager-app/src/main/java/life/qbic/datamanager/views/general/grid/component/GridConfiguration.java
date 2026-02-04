package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

public interface GridConfiguration<T, F> {

  sealed interface ConfiguredGrid<T, F> extends Filterable<F>, ItemCountChangePublisher permits
      InMemoryGrid, LazyGrid {


  }

  interface FilterTester<T, F> extends BiPredicate<T, F> {

  }

  final class InMemoryGrid<T, F> implements ConfiguredGrid<T, F> {

    private final Grid<T> grid;
    private final FilterTester<T, F> filterTester;

    public InMemoryGrid(Grid<T> grid, List<T> items, FilterTester<T, F> filterTester) {
      this.grid = Objects.requireNonNull(grid);
      this.filterTester = Objects.requireNonNull(filterTester);
      Objects.requireNonNull(items);
      grid.setItems(items);
    }

    @Override
    public void setFilter(F filter) {
      grid.getListDataView().setFilter(item -> filterTester.test(item, filter));
    }

    @Override
    public Registration addItemCountChangeListener(
        ComponentEventListener<ItemCountChangeEvent<?>> listener) {
      return grid.getListDataView().addItemCountChangeListener(listener);
    }
  }

  final class LazyGrid<T, F> implements ConfiguredGrid<T, F> {

    private final Grid<T> grid;
    private final ConfigurableFilterDataProvider<T, Void, F> dataProvider;

    public LazyGrid(Grid<T> grid,
        ConfigurableFilterDataProvider<T, Void, F> dataProvider) {
      this.grid = Objects.requireNonNull(grid);
      this.dataProvider = Objects.requireNonNull(dataProvider);
      grid.setItems(dataProvider);
    }

    @Override
    public void setFilter(F filter) {
      dataProvider.setFilter(filter);
    }

    @Override
    public Registration addItemCountChangeListener(
        ComponentEventListener<ItemCountChangeEvent<?>> listener) {
      return grid.getLazyDataView().addItemCountChangeListener(listener);
    }
  }

  ConfiguredGrid<T, F> applyConfiguration(Grid<T> grid);

}
