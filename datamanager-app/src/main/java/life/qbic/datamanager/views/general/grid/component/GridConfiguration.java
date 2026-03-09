package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.shared.SelectionPreservationMode;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.selection.SelectionModel;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Defines a configuration strategy for a Vaadin {@link Grid} that encapsulates how data is
 * provided, filtered, and interacted with.
 *
 * <p><b>Intention</b></p>
 * <p>
 * This interface acts as a {@code Strategy} (see refactoring.guru) for applying a specific grid
 * setup, such as in-memory or lazy-loaded data handling. It produces a {@link ConfiguredGrid} that
 * exposes a simplified API for common operations like filtering, selection handling, and item count
 * observation.
 * </p>
 *
 * <p>
 * The resulting {@link ConfiguredGrid} also works as a small {@code Facade}, hiding Vaadin-specific
 * implementation details from the rest of the application and providing a consistent abstraction
 * regardless of how the data is backed.
 * </p>
 *
 * <p>
 * Typical use cases:
 * <ul>
 *   <li>Apply a unified grid setup in multiple views</li>
 *   <li>Switch between in-memory and lazy data providers without changing client code</li>
 *   <li>Centralize grid-related behavior (filtering, selection, paging)</li>
 * </ul>
 * </p>
 *
 * @param <T> the grid item type
 * @param <F> the filter type used to restrict visible items
 */
public interface GridConfiguration<T, F> {


  /**
   * Applies this configuration strategy to the given {@link Grid} instance and returns a wrapped
   * {@link ConfiguredGrid} that exposes higher-level operations.
   *
   * <p>
   * Implementations decide how data is attached (e.g. in-memory list vs. lazy data provider), how
   * filtering is performed, and how the grid should behave.
   * </p>
   *
   * @param grid the grid to configure
   * @return a configured and wrapped grid abstraction
   */
  ConfiguredGrid<T, F> applyConfiguration(Grid<T> grid);

  /**
   * Functional interface used to evaluate whether an item matches a given filter.
   *
   * <p>
   * This is primarily used by in-memory configurations where filtering must be performed on the
   * client-side by testing each item against the provided filter.
   * </p>
   *
   * @param <T> item type
   * @param <F> filter type
   */
  interface FilterTester<T, F> extends BiPredicate<T, F> {

  }

  /**
   * Abstraction over the grid's selection model to avoid leaking Vaadin-specific APIs to
   * consumers.
   *
   * <p>
   * Provides only the operations commonly needed by the application layer.
   * </p>
   *
   * @param <T> item type
   */
  interface ConfiguredSelectionModel<T> {

    void deselectAll();


    /**
     * Returned items conform with {@link SelectionModel#getSelectedItems()}
     *
     * @return all currently selected items
     * @see SelectionModel#getSelectedItems()
     */
    Set<T> getSelectedItems();

    /**
     * Registers a listener for selection changes.
     *
     * @param listener the listener to register
     * @return a handle that can be used to remove the listener
     * @see Grid#addSelectionListener(SelectionListener)
     */
    Registration addSelectionListener(SelectionListener<Grid<T>, T> listener);
  }


  interface Filterable<F> {

    /**
     * Applies the given filter to the underlying grid.
     *
     * <p>
     * This method delegates to the active {@link GridConfiguration} strategy, which determines how
     * the filter is interpreted and applied:
     * <ul>
     *   <li>Lazy configurations forward the filter to a backend-aware data provider</li>
     *   <li>In-memory configurations evaluate the filter against each item</li>
     * </ul>
     * </p>
     *
     * <p>
     * This method does not perform filtering itself; it acts as a pass-through
     * entry point defined by the Facade.
     * </p>
     *
     * @param filter the filter to apply, may be {@code null} depending on the strategy
     */
    void setFilter(F filter);
  }

  interface ItemCountChangePublisher {

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

  /**
   * Base abstraction representing a fully configured grid instance.
   *
   * <p>
   * This class wraps a {@link Grid} and exposes a stable, framework-shielded API for:
   * <ul>
   *   <li>Filtering ({@link Filterable})</li>
   *   <li>Selection handling ({@link ConfiguredSelectionModel})</li>
   *   <li>Item count change observation ({@link ItemCountChangePublisher})</li>
   * </ul>
   * </p>
   *
   * <p>
   * Implementations define how data is provided and how filtering is applied
   * (e.g. in-memory vs. lazy backend).
   * </p>
   *
   * @param <T> item type
   * @param <F> filter type
   */
  abstract sealed class ConfiguredGrid<T, F> extends Composite<Div> implements
      Filterable<F>, ItemCountChangePublisher,
      ConfiguredSelectionModel<T> permits InMemoryGrid, LazyGrid {

    protected final Grid<T> grid;

    protected ConfiguredGrid(Grid<T> grid) {
      this.grid = Objects.requireNonNull(grid);
      configureGridForMultiSelect(this.grid);
      getContent().add(grid);
    }

    @Override
    protected Div initContent() {
      var div = super.initContent();
      div.addClassNames("display-contents");
      return div;
    }


    /**
     * Applies a consistent multi-selection setup to the grid.
     *
     * <p>
     * Ensures:
     * <ul>
     *   <li>Multi-select mode is enabled</li>
     *   <li>Selections are preserved across data refreshes</li>
     *   <li>A visible "select all" checkbox is shown</li>
     *   <li>The selection column remains frozen</li>
     * </ul>
     * </p>
     */
    protected static <T> void configureGridForMultiSelect(Grid<T> grid) {
      grid.setSelectionMode(SelectionMode.MULTI);
      grid.setSelectionPreservationMode(SelectionPreservationMode.PRESERVE_ALL);
      GridMultiSelectionModel<T> selectionModel = ((GridMultiSelectionModel<T>) grid.getSelectionModel());
      selectionModel.setSelectAllCheckboxVisibility(
          GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE);
      selectionModel.setSelectionColumnFrozen(true);
    }


    /**
     * @return the current number of items known to the data communicator or fetches the total
     * number if unknown
     * @see DataCommunicator#getItemCount()
     */
    public int getItemCount() {
      return grid.getDataCommunicator().getItemCount();
    }

    /**
     * Refreshes all items from the underlying data provider.
     *
     * @see DataProvider#refreshAll()
     */
    public void refreshAll() {
      grid.getDataProvider().refreshAll();
    }

    /**
     * @return the list of columns currently configured on the grid
     * @see Grid#getColumns()
     */
    public List<Grid.Column<T>> getColumns() {
      return grid.getColumns();
    }

    @Override
    public void deselectAll() {
      grid.getSelectionModel().deselectAll();
    }

    @Override
    public Set<T> getSelectedItems() {
      return grid.getSelectionModel().getSelectedItems();
    }

    @Override
    public Registration addSelectionListener(SelectionListener<Grid<T>, T> listener) {
      return grid.addSelectionListener(listener);
    }
  }

  /**
   * Grid configuration that operates on an in-memory collection of items.
   *
   * <p>
   * Filtering is performed locally by evaluating each item using a provided {@link FilterTester}.
   * </p>
   */
  final class InMemoryGrid<T, F> extends ConfiguredGrid<T,F> {

    private final FilterTester<T, F> filterTester;

    /**
     * Creates an in-memory grid backed by the given items.
     *
     * @param grid the grid to configure
     * @param items the full set of items to display
     * @param filterTester predicate used to evaluate whether an item matches a filter
     */
    public InMemoryGrid(Grid<T> grid, List<T> items, FilterTester<T, F> filterTester) {
      super(grid);
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


  /**
   * Grid configuration that uses a lazy, backend-backed data provider.
   *
   * <p>
   * Filtering is delegated to the {@link ConfigurableFilterDataProvider}, allowing server-side
   * querying, paging, and scaling to large datasets.
   * </p>
   */
  final class LazyGrid<T, F> extends ConfiguredGrid<T, F> {

    private static final int DEFAULT_QUERY_SIZE = 150;
    private static final int MAX_QUERY_SIZE = 350;


    /**
     * Creates a lazily loaded grid backed by a configurable data provider.
     *
     * @param grid the grid to configure
     * @param dataProvider the backend-aware data provider used for fetching items
     */
    private final ConfigurableFilterDataProvider<T, Void, F> dataProvider;

    public LazyGrid(Grid<T> grid,
        ConfigurableFilterDataProvider<T, Void, F> dataProvider) {
      super(grid);
      this.dataProvider = Objects.requireNonNull(dataProvider);
      grid.setItems(dataProvider);
      var computedPageSize = Math.clamp(DEFAULT_QUERY_SIZE, 100, MAX_QUERY_SIZE);
      grid.setPageSize(computedPageSize);
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
}
