package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.function.BiPredicate;

public class GridConfigurer {

  public static <T, F> GridFilterConfiguration<T, F> configureLazy(Grid<T> grid,
      FetchCallback<T, F> fetchCallback,
      CountCallback<T, F> countCallback) {
    ConfigurableFilterDataProvider<T, Void, F> dataProvider = DataProvider.fromFilteringCallbacks(
            fetchCallback, countCallback)
        .withConfigurableFilter();
    grid.setItems(dataProvider);
    return new LazyConfiguration<>(grid, dataProvider);
  }

  public static <T, F> GridFilterConfiguration<T, F> configureInMemory(
      Grid<T> grid,
      List<T> items,
      FilterTester<T, F> filterTester) {
    grid.setItems(items);
    return new InMemoryConfiguration<>(grid, filterTester);
  }

  @FunctionalInterface
  public interface FilterTester<T, F> extends BiPredicate<T, F> {

    @Override
    boolean test(T element, F filter);
  }

  static class LazyConfiguration<T, F> implements GridFilterConfiguration<T, F> {

    private final Grid<T> grid;
    private final ConfigurableFilterDataProvider<T, Void, F> dataProvider;

    LazyConfiguration(Grid<T> grid, ConfigurableFilterDataProvider<T, Void, F> dataProvider) {
      this.grid = grid;
      this.dataProvider = dataProvider;
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

    @Override
    public Grid<T> getGrid() {
      return grid;
    }

  }

  static class InMemoryConfiguration<T, F> implements GridFilterConfiguration<T, F> {

    private final Grid<T> grid;
    private final FilterTester<T, F> filterTester;

    InMemoryConfiguration(Grid<T> grid, FilterTester<T, F> filterTester) {
      this.grid = grid;
      this.filterTester = filterTester;
    }

    @Override
    public void setFilter(F filter) {
      grid.getListDataView().setFilter(it -> filterTester.test(it, filter));
    }

    @Override
    public Registration addItemCountChangeListener(
        ComponentEventListener<ItemCountChangeEvent<?>> listener) {
      return grid.getListDataView().addItemCountChangeListener(listener);
    }

    @Override
    public Grid<T> getGrid() {
      return getGrid();
    }

  }


}
