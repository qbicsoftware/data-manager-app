package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.shared.Registration;

/**
 * Abstraction for applying filters and observing item count changes on a {@link Grid}, independent
 * of the underlying data source.
 *
 * <p>
 * This interface represents the <b>Strategy</b> role in the configuration pattern used by
 * {@link GridFilterStrategyFactory}. Concrete implementations adapt different Vaadin data access
 * mechanisms (lazy vs. in-memory) to a common API.
 * </p>
 *
 * <p>
 * Clients can use this interface to:
 * <ul>
 *   <li>Apply filters without knowing how filtering is implemented</li>
 *   <li>Listen to item count changes in a uniform way</li>
 *   <li>Access the configured grid instance</li>
 * </ul>
 * </p>
 *
 * <b>Attention: A change to the {@link com.vaadin.flow.data.provider.DataProvider} after grid configuration triggers a reconfiguration.</b>
 *
 * @param <T> the grid item type
 * @param <F> the filter type
 */
public interface GridFilterStrategy<T, F> {

  /**
   * Applies the given filter to the underlying grid data.
   *
   * <p>
   * The concrete behavior depends on the implementation:
   * <ul>
   *   <li>Lazy configurations forward the filter to the data provider</li>
   *   <li>In-memory configurations evaluate the filter against each item</li>
   * </ul>
   * </p>
   *
   * @param filter the filter to apply, may be {@code null} depending on implementation
   */

  void setFilter(F filter);

  /**
   * Registers a listener that is notified when the number of items available to the grid changes.
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

  /**
   * Returns the configured {@link Grid} instance.
   *
   * <p>
   * This method allows fluent-style usage where additional grid
   * configuration is applied after the data and filtering behavior
   * have been set up.
   * </p>
   *
   * @return the configured grid
   */
  Grid<T> getGrid();

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
  class IncompatibleGridDataProviderException extends RuntimeException {

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
