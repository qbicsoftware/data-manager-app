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

public class FilterGridConfigurations {

  /**
   * Creates and applies a lazy-loading filter strategy for the given grid.
   *
   * <p>
   * This method configures the grid with a callback-based {@link ConfigurableFilterDataProvider}
   * and returns a {@link ConfiguredGrid} that delegates filtering and item count observation to
   * the grid's lazy data view.
   * </p>
   *
   * <p>
   * The returned strategy assumes that the grid remains backed by a non in-memory
   * {@link DataProvider}. If the grid's data provider is replaced after configuration, the strategy
   * will throw an {@link IncompatibleGridDataProviderException}.
   * </p>
   *
   * @param grid          the grid to configure for lazy data access
   * @param fetchCallback callback used to fetch items for a query
   * @param countCallback callback used to determine the total item count
   * @param <T>           the grid item type
   * @param <F>           the filter type
   * @return a filter strategy for lazy-loaded grid data
   */
  public static <T, F> ConfiguredGrid<T, F> configureLazy(Grid<T> grid,
      FetchCallback<T, F> fetchCallback,
      CountCallback<T, F> countCallback) {
    ConfigurableFilterDataProvider<T, Void, F> dataProvider = DataProvider.fromFilteringCallbacks(
            fetchCallback, countCallback)
        .withConfigurableFilter();
    grid.setItems(dataProvider);
    return new LazyStrategy<>(grid, dataProvider);
  }

  /**
   * Creates and applies an in-memory filter strategy for the given grid.
   *
   * <p>
   * This method configures the grid with the provided items and returns a
   * {@link ConfiguredGrid} that applies filtering by evaluating each item using the supplied
   * {@link FilterTester}.
   * </p>
   *
   * <p>
   * The returned strategy assumes that the grid remains backed by an in-memory
   * {@link DataProvider}. If the grid's data provider is replaced after configuration, the strategy
   * will throw an {@link IncompatibleGridDataProviderException}.
   * </p>
   *
   * @param grid         the grid to configure for in-memory filtering
   * @param items        the unfiltered items shown in the grid
   * @param filterTester predicate used to test items against the filter
   * @param <T>          the grid item type
   * @param <F>          the filter type
   * @return a filter strategy for in-memory grid data
   */

  public static <T, F> ConfiguredGrid<T, F> configureInMemory(
      Grid<T> grid,
      List<T> items,
      FilterTester<T, F> filterTester) {
    grid.setItems(items);
    return new InMemoryConfiguration<>(grid, filterTester);
  }

  /**
   * Functional interface used to evaluate whether a grid item matches
   * a given filter.
   *
   * <p>
   * This interface exists primarily to give semantic meaning to the
   * {@link BiPredicate} used for in-memory filtering.
   * </p>
   *
   * @param <T> the grid item type
   * @param <F> the filter type
   */
  @FunctionalInterface
  public interface FilterTester<T, F> extends BiPredicate<T, F> {

    @Override
    boolean test(T element, F filter);
  }

  /**
   * {@link ConfiguredGrid} implementation for lazily loaded grid data.
   *
   * <p>
   * This strategy configures and manages a {@link Grid} that retrieves its data on demand using
   * Vaadin callback-based data providers.
   * </p>
   *
   * <p>
   * Filtering is applied by forwarding the filter object directly to the underlying
   * {@link ConfigurableFilterDataProvider}. Item count changes are observed through the grid's lazy
   * data view.
   * </p>
   *
   * <p>
   * This strategy assumes that the grid remains backed by a lazy data provider for its entire
   * lifetime. If the grid's data provider is replaced with an in-memory or otherwise incompatible
   * provider after configuration, this strategy will throw an
   * {@link IncompatibleGridDataProviderException} to signal a violation of its
   * operational assumptions.
   * </p>
   *
   * <p>
   * Typical use cases include large datasets, remote services, and database-backed grids where
   * loading all items into memory is impractical.
   * </p>
   *
   * @param <T> the grid item type
   * @param <F> the filter type
   */
  static class LazyStrategy<T, F> implements ConfiguredGrid<T, F> {

    private final Grid<T> grid;
    private final ConfigurableFilterDataProvider<T, Void, F> dataProvider;

    LazyStrategy(Grid<T> grid, ConfigurableFilterDataProvider<T, Void, F> dataProvider) {
      this.grid = grid;
      validateDataProvider();
      this.dataProvider = dataProvider;
    }

    @Override
    public void setFilter(F filter) {
      validateDataProvider();
      dataProvider.setFilter(filter);
    }

    private void validateDataProvider() throws IncompatibleGridDataProviderException {
      if (this.grid.getDataProvider().isInMemory()) {
        throw new IncompatibleGridDataProviderException(
            "In Memory grid data unexpected. Changes to the grid after configuration. " + grid);
      }
    }

    @Override
    public Grid<T> grid() {
      return this.grid;
    }

    @Override
    public Registration addItemCountChangedListener( ComponentEventListener<ItemCountChangeEvent<?>> listener) {
      validateDataProvider();
      return grid.getLazyDataView().addItemCountChangeListener(listener);
    }
  }

  /**
   * {@link ConfiguredGrid} implementation for in-memory grid data.
   *
   * <p>
   * This strategy configures and manages a {@link Grid} whose items are fully loaded into memory
   * and exposed through a {@code com.vaadin.flow.data.provider.ListDataView}.
   * </p>
   *
   * <p>
   * Filtering is performed by applying a predicate derived from a
   * {@link FilterGridConfigurations.FilterTester} to the grid's in-memory data view. Item count
   * changes are detected directly from the filtered data set.
   * </p>
   *
   * <p>
   * This strategy assumes that the grid remains backed by an in-memory data provider. If the grid's
   * data provider is replaced with a lazy or otherwise incompatible provider after configuration,
   * this strategy will throw an {@link IncompatibleGridDataProviderException} to
   * signal a misuse of the API.
   * </p>
   *
   * <p>
   * This strategy is intended for small to medium-sized datasets where all items can be safely held
   * in memory and filtering can be performed locally.
   * </p>
   *
   * @param <T> the grid item type
   * @param <F> the filter type
   */
  static class InMemoryConfiguration<T, F> implements ConfiguredGrid<T, F> {

    private final Grid<T> grid;
    private final FilterTester<T, F> filterTester;

    InMemoryConfiguration(Grid<T> grid, FilterTester<T, F> filterTester) {
      this.grid = grid;
      validateDataProvider();
      this.filterTester = filterTester;
    }

    private void validateDataProvider() throws IncompatibleGridDataProviderException {
      if (!this.grid.getDataProvider().isInMemory()) {
        throw new IncompatibleGridDataProviderException(
            "Lazy grid data unexpected. Changes to the grid after configuration. " + grid);
      }
    }

    @Override
    public void setFilter(F filter) {
      validateDataProvider();
      grid.getListDataView().setFilter(it -> filterTester.test(it, filter));
    }

    @Override
    public Grid<T> grid() {
      return grid;
    }

    @Override
    public Registration addItemCountChangedListener(
        ComponentEventListener<ItemCountChangeEvent<?>> listener) {
      validateDataProvider();
      return grid.getListDataView().addItemCountChangeListener(listener);
    }
  }

  /**
   * Runtime exception indicating that the {@link Grid} is backed by an unexpected or incompatible
   * data provider type.
   *
   * <p>
   * This exception is thrown by {@code GridFilterStrategy}  implementations when they detect that
   * the grid's data provider does not match the expected type.
   * </p>
   *
   * <p>
   * The filtering strategies assume a stable data provider type:
   * <ul>
   *   <li>Lazy-loading strategies require {@link DataProvider#isInMemory()} to be {@code false} for the configured {@link Grid}</li>
   *   <li>In-memory strategies require {@link DataProvider#isInMemory()} to be {@code true} for the configured {@link Grid}</li>
   * </ul>
   * </p>
   *
   * <p>
   * If the grid is reconfigured with a different data provider after the
   * strategy has been applied, the internal assumptions of the strategy
   * are violated. Rather than failing silently or producing inconsistent
   * filtering behavior, this exception is thrown to signal a programming error.
   * </p>
   *
   * <p>
   * This exception is unchecked because such a mismatch indicates an
   * invalid application state caused by incorrect API usage rather than
   * a recoverable runtime condition.
   * </p>
   */
  public static class IncompatibleGridDataProviderException extends RuntimeException {

    /**
     * Creates a new exception indicating an incompatible grid data provider.
     *
     * <p>
     * The message should describe the expected data provider type and the condition that caused the
     * mismatch.
     * </p>
     *
     * @param message a human-readable explanation of the data provider mismatch
     */
    public IncompatibleGridDataProviderException(String message) {
      super(message);
    }
  }


}
