package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.grid.component.GridConfiguration.FilterTester;
import org.springframework.lang.NonNull;

public class FilterGridConfigurations {

  private FilterGridConfigurations() {

  }

  public static <T, F> GridConfiguration<T, F> lazy(
      FetchCallback<T, F> fetchCallback,
      CountCallback<T, F> countCallback
  ) {
    return new LazyFilterGridConfiguration<>(fetchCallback, countCallback);
  }

  public static <T, F> GridConfiguration<T, F> inMemory(
      List<T> items, FilterTester<T, F> filterTester) {
    return new InMemoryFilterGridConfiguration<>(items, filterTester);
  }


  static class LazyFilterGridConfiguration<T, F> implements
      GridConfiguration<T, F> {

    private final ConfigurableFilterDataProvider<T, Void, F> dataProvider;

    public LazyFilterGridConfiguration(
        @NonNull FetchCallback<T, F> fetchCallback,
        @NonNull CountCallback<T, F> countCallback) {

      Objects.requireNonNull(fetchCallback);
      Objects.requireNonNull(countCallback);

      this.dataProvider = DataProvider.fromFilteringCallbacks(fetchCallback, countCallback)
          .withConfigurableFilter();
    }

    @Override
    public ConfiguredGrid<T, F> applyConfiguration(Grid<T> grid) {
      return new LazyGrid<>(grid, dataProvider);
    }
  }

  static class InMemoryFilterGridConfiguration<T, F> implements
      GridConfiguration<T, F> {

    private final List<T> items;
    private final FilterTester<T, F> filterTester;

    InMemoryFilterGridConfiguration(List<T> items, FilterTester<T, F> filterTester) {
      this.items = Objects.requireNonNull(items);
      this.filterTester = Objects.requireNonNull(filterTester);
    }

    @Override
    public ConfiguredGrid<T, F> applyConfiguration(Grid<T> grid) {
      return new InMemoryGrid<>(grid, items, filterTester);
    }
  }

}
