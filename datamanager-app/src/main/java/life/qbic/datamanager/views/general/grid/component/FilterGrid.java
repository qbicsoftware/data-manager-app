package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.shared.SelectionPreservationMode;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider.CountCallback;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * A filter grid comes with some improvements for the user and decorates a plain
 * {@link Grid}.
 * <p>
 * Some of the improvements are:
 *
 * <ul>
 *   <li>selection column - shows checkboxes to select multiple rows at once</li>
 *   <li>search input field - to look-up for certain terms</li>
 *   <li>show/hide column - a menu to toggle column visibility</li>
 *   <li>secondary action group - to set up a contextual actions for the user that are semantically linked to the selected items</li>
 *   <li>selected items display - a visual indicator about how many items are selected</li>
 * </ul>
 *
 * @param <T> the type of items displayed in the filter grid
 * @param <F> the type of filter used in the UI
 * @since 1.12.0
 */
public final class FilterGrid<T, F> extends Div {

  static final String FLEX_HORIZONTAL_CSS = "flex-horizontal";
  static final String GAP_04_CSS = "gap-04";

  private static final int DEFAULT_QUERY_SIZE = 150;
  private static final int MAX_QUERY_SIZE = 350;
  private static final String DEFAULT_ITEM_DISPLAY_LABEL = "item";
  private static final Logger log = LoggerFactory.logger(FilterGrid.class);

  private final Class<T> type;
  private final Class<F> filterType;

  private final Grid<T> grid;
  private final Div selectionDisplay = new SelectionNotification();
  private final Div secondaryActionGroup = createSecondaryActionGroup();

  // make sure the search field exists
  private final TextField searchField = createSearchField();

  private String currentItemDisplayLabel = DEFAULT_ITEM_DISPLAY_LABEL;


  /**
   * Factory method for creating a {@link FilterGrid} based on an in-memory {@link List}
   *
   * @param itemType                 the type of item for the {@link FilterGrid}
   * @param filterType               the type of filter for the {@link FilterGrid}
   * @param grid                     the grid to use for display. This grid defines the columns and
   *                                 their properties. The data provider in this grid is
   *                                 overwritten.
   * @param filterSupplier           supplies a filter that can be combined with a search term using
   *                                 the {@link SearchTermFilterCombiner}.
   * @param items                    the list of items containing the data for the grid
   * @param filterTester             a {@link FilterTester} to filter data based on the current
   *                                 filter
   * @param searchTermFilterCombiner a combiner to combine the filter supplied by filterSupplier
   *                                 with the search term in the grid
   * @param <T>                      the item type
   * @param <F>                      the filter type
   *
   *
   *
   *                                 <h3>Example</h3>
   *                                 <pre>
   *                                                                 {@code
   *                                  // Create a grid instance for displaying items
   *                                  Grid<MyItem> grid = new Grid<>(MyItem.class);
   *                                  grid.addColumn(myItem -> myItem.title())
   *                                     .setKey("itemTitle")
   *                                     .setHeader("Item Title")
   *                                     .setSortable(false)
   *                                     .setAutoWidth(true);
   *
   *                                  // Define a filter supplier and filter tester
   *                                  Supplier<MyFilter> filterSupplier = () -> new MyFilter();
   *                                  FilterTester<MyItem, MyFilter> filterTester = (item, filter) -> {
   *                                      // Implement filtering logic
   *                                      return filter.matches(item);
   *                                  };
   *
   *                                  // Create a list of items
   *                                  List<MyItem> items = Arrays.asList(new MyItem(...), new MyItem(...));
   *
   *                                  // Create a search term filter combiner
   *                                  SearchTermFilterCombiner<MyFilter> combiner = (searchTerm, filter) ->
   *                                    {
   *                                      //Modify or replace filter
   *                                      return filter.withSearchTerm(searchTerm);
   *                                    };
   *
   *                                  // Create the FilterGrid using the inMemory method
   *                                  FilterGrid<MyItem, MyFilter> filterGrid = FilterGrid.inMemory(
   *                                      MyItem.class,
   *                                      MyFilter.class,
   *                                      grid,
   *                                      filterSupplier,
   *                                      items,
   *                                      filterTester,
   *                                      combiner
   *                                  );
   *                                  }
   *                                 </pre>
   * @return a configured {@link FilterGrid}
   */
  public static <T, F> FilterGrid<T, F> inMemory(
      Class<T> itemType,
      Class<F> filterType,
      Grid<T> grid,
      Supplier<F> filterSupplier,
      List<T> items,
      FilterTester<T, F> filterTester,
      SearchTermFilterCombiner<F> searchTermFilterCombiner
  ) {
    return new FilterGrid<>(itemType, filterType, grid, filterSupplier, items, filterTester,
        searchTermFilterCombiner);
  }


  /**
   * Creates and configures a {@link FilterGrid} using a lazy loading mechanism based on a specified
   * {@link FetchCallback} and {@link CountCallback}. This method allows for efficient retrieval of
   * items based on filter criteria and pagination.
   *
   * @param <T> the type of items to be displayed in the {@link FilterGrid}
   * @param <F> the type of filter used for the {@link FilterGrid}
   * @param itemType the {@link Class} representation of the item type for the grid
   * @param filterType the {@link Class} representation of the filter type for the grid
   * @param grid a {@link Grid} instance that defines the grid's columns and their properties.
   * @param filterSupplier a {@link Supplier} that generates a filter instance, which can be combined
   *                       with a search term using the {@link SearchTermFilterCombiner}.
   * @param fetchCallback a {@link FetchCallback} that handles the retrieval of items based on the
   *                      current filter and pagination settings.
   * @param countCallback a {@link CountCallback} that provides the total count of items that meet the
   *                      filter criteria, useful for pagination.
   * @param searchTermFilterCombiner a {@link SearchTermFilterCombiner} responsible for combining
   *                                 the filter provided by {@code filterSupplier} with the search term
   *                                 entered in the grid.
   * @return a fully configured {@link FilterGrid<T, F>} instance, ready for display and interaction.
   *
   * @throws IllegalArgumentException if any required parameter is null.
   * @throws UnsupportedOperationException if the provided grid is not compatible with the item type.
   *
   * <h3>Example</h3>
   * <pre>
   * {@code
   *  // Create a grid instance for displaying items
   *  Grid<MyItem> grid = new Grid<>(MyItem.class);
   *
   *  // Define a filter supplier
   *  Supplier<MyFilter> filterSupplier = () -> new MyFilter();
   *
   *  // Create a fetch callback to retrieve items
   *  FetchCallback<MyItem, MyFilter> fetchCallback = query -> {
   *      // Logic to fetch items based on filter, offset, and limit
   *      return myItemService.fetchItems(query.getFilter(), query.getOffset(), query.getLimit());
   *  };
   *
   *  // Create a count callback to get the total count of items
   *  CountCallback<MyItem, MyFilter> countCallback = query -> {
   *      // Logic to count items based on filter
   *      return myItemService.countItems(query.getFilter());
   *  };
   *
   *  // Create a search term filter combiner
   *  SearchTermFilterCombiner<MyFilter> combiner = new MySearchTermFilterCombiner();
   *
   *  // Create the FilterGrid using the lazy method
   *  FilterGrid<MyItem, MyFilter> filterGrid = FilterGrid.lazy(
   *      MyItem.class,
   *      MyFilter.class,
   *      grid,
   *      filterSupplier,
   *      fetchCallback,
   *      countCallback,
   *      combiner
   *  );
   * }
   * </pre>
   */
  public static <T, F> FilterGrid<T, F> lazy(Class<T> itemType,
      Class<F> filterType,
      Grid<T> grid,
      Supplier<F> filterSupplier,
      FetchCallback<T, F> fetchCallback,
      CountCallback<T, F> countCallback,
      SearchTermFilterCombiner<F> searchTermFilterCombiner) {
    return new FilterGrid<>(itemType, filterType, grid, filterSupplier, fetchCallback,
        countCallback, searchTermFilterCombiner);
  }

  private FilterGrid(
      Class<T> itemType,
      Class<F> filterType,
      Grid<T> grid,
      Supplier<F> filterSupplier,
      List<T> items,
      FilterTester<T, F> filterTester,
      SearchTermFilterCombiner<F> searchTermFilterUpdater) {
    //assign fields
    this.type = Objects.requireNonNull(itemType);
    this.filterType = Objects.requireNonNull(filterType);
    this.grid = Objects.requireNonNull(grid);

    //set items
    grid.setItems(items);
    configureGrid(grid);
    //construct filter Grid component
    constructComponent(grid);

    listenToSelection();
    //update the filter
    searchField.addValueChangeListener(
        event -> updateInMemoryFilter(searchTermFilterUpdater, filterTester, filterSupplier.get(),
            event.getValue()));
  }

  /**
   * Refreshes the grid and clears the selection.
   */
  public void refreshAll() {
    this.grid.getDataProvider().refreshAll();
    this.grid.deselectAll();
  }

  /**
   * A functional interface that represents a generic filter testing mechanism for evaluating
   * whether an element matches a specific filter criteria.
   *
   * <p>This interface extends the standard {@link java.util.function.BiPredicate}
   * to provide a more specialized contract for testing elements against filters. It allows for
   * flexible and reusable filtering logic across different types.</p>
   *
   * <h2>Purpose</h2>
   * <p>The primary purpose of this interface is to encapsulate the logic for
   * testing whether a given element satisfies a specific filter condition.</p>
   *
   * <h2>Generic Type Parameters</h2>
   * <ul>
   *   <li><b>T</b>: The type of the element being tested</li>
   *   <li><b>F</b>: The type of the filter used for testing</li>
   * </ul>
   *
   * <h2>Usage Example</h2>
   * <pre>
   * FilterTester&lt;String, Pattern&gt; regexFilter = (element, filter) -&gt;
   *     filter.matcher(element).matches();
   *
   * FilterTester&lt;Integer, Range&gt; rangeFilter = (value, range) -&gt;
   *     value &gt;= range.getMin() && value &lt;= range.getMax();
   *
   * FilterTester&lt;Person, String&gt; personFilter = (element, searchTerm) -&gt;
   *     element.firstName().contains(searchTerm) || element.lastName().contains(searchTerm)
   * </pre>
   *
   * <h2>Contract</h2>
   * <p>Implementations must provide a consistent and predictable method
   * for testing whether an element matches a given filter.</p>
   *
   * @param <T> the type of element to be tested
   * @param <F> the type of filter used in the testing process
   */
  @FunctionalInterface
  public interface FilterTester<T, F> extends BiPredicate<T, F> {

    @Override
    boolean test(T element, F filter);
  }


  /**
   * A functional interface that combines a search term with an existing filter, producing a filter
   * that incorporates the search term's context.
   *
   * <p>This interface extends {@link java.util.function.BiFunction} to provide
   * a mechanism for transforming filters based on search terms.</p>
   *
   * <h2>Purpose</h2>
   * <p>The primary goal of this interface is to create a filter that integrates
   * a search term's context, without specifying whether this is done through modification or by
   * creating a new filter instance.</p>
   *
   * <h2>Generic Type Parameters</h2>
   * <ul>
   *   <li><b>F</b>: The type of the filter to be transformed</li>
   * </ul>
   *
   * <h2>Behavior</h2>
   * <p>Implementations may:</p>
   * <ul>
   *   <li>Modify the existing filter in-place</li>
   *   <li>Create and return a new filter instance</li>
   *   <li>Return the original filter unchanged if no transformation is needed</li>
   * </ul>
   *
   * <h2>Usage Example</h2>
   * <pre>
   * SearchTermFilterCombiner&lt;DateRange&gt; dateRangeTransformer = (searchTerm, existingFilter) -&gt; {
   *     // Could return a modified filter or a new filter instance
   *     return searchTerm.contains("recent")
   *         ? existingFilter.withMaxAge(30)
   *         : existingFilter;
   * };
   *
   * </pre>
   *
   * <h2>Contract</h2>
   * <p>Implementations must provide a consistent method for transforming
   * a filter based on a search term, with no guarantee of whether the original
   * filter is modified or a new instance is created.</p>
   *
   * @param <F> the type of filter to be transformed with a search term
   */
  @FunctionalInterface
  public interface SearchTermFilterCombiner<F> extends BiFunction<String, F, F> {

    /**
     * Combines a filter and search term into a filter respecting the search term.
     *
     * @param searchTerm the search term to integrate into the filter
     * @param filter     the filter without the search term
     * @return a filter that respects the search term
     */
    F combineWithSearchTerm(String searchTerm, F filter);

    /**
     * {@inheritDoc}
     *
     * @see #combineWithSearchTerm(String, Object)
     */
    @Override
    default F apply(String searchTerm, F filter) {
      return combineWithSearchTerm(searchTerm, filter);
    }
  }

  private FilterGrid(
      Class<T> itemType,
      Class<F> filterType,
      Grid<T> grid,
      Supplier<F> filterSupplier,
      FetchCallback<T, F> fetchCallback,
      CountCallback<T, F> countCallback,
      SearchTermFilterCombiner<F> searchTermFilterCombiner) {
    //assign fields
    this.type = Objects.requireNonNull(itemType);
    this.filterType = Objects.requireNonNull(filterType);
    this.grid = Objects.requireNonNull(grid);

    //set items
    ConfigurableFilterDataProvider<T, Void, F> dataProvider = DataProvider.fromFilteringCallbacks(
            fetchCallback, countCallback)
        .withConfigurableFilter();
    grid.setItems(dataProvider);
    configureGrid(grid);
    //construct filter Grid component
    constructComponent(grid);

    listenToSelection();

    //update the filter
    searchField.addValueChangeListener(event ->
        updateFilter(searchTermFilterCombiner, dataProvider, filterSupplier.get(),
            event.getValue()));

  }

  private void constructComponent(Grid<T> grid) {
    var primaryGridControls = getPrimaryGridControls(grid.getColumns());
    add(primaryGridControls, grid);
    addClassNames("flex-vertical", "gap-03", "height-full", "width-full");

  }

  private void listenToSelection() {
    updateSelectionDisplay(this.grid.getSelectionModel().getSelectedItems().size());
    addSelectionListener(event -> updateSelectionDisplay(event.selectedItems().size()));
  }

  private static <T> void configureGrid(Grid<T> grid) {
    configureGridForMultiSelect(grid);
    optimizeGrid(grid, DEFAULT_QUERY_SIZE);
  }

  /**
   * Updates the filter and fires a
   * {@link life.qbic.datamanager.views.general.grid.component.FilterGrid.FilterUpdateEvent}
   *
   * @param searchTermFilterCombiner the function that updates the filter based on a search term
   * @param dataProvider            the dataprovider to update the filter with
   * @param filter                  the filter to update
   * @param searchTerm              the search term to use
   */
  private void updateFilter(SearchTermFilterCombiner<F> searchTermFilterCombiner,
      ConfigurableFilterDataProvider<T, Void, F> dataProvider,
      F filter,
      String searchTerm) {
    if (grid.getDataProvider().isInMemory()) {
      log.warn(
          "In memory data provider found but expected lazy data provider. Ignoring filter term");
      return;
    }
    F updatedFilter = searchTermFilterCombiner.apply(searchTerm, filter);
    dataProvider.setFilter(updatedFilter);
    fireEvent(new FilterUpdateEvent<>(this.filterType, this, false, filter, updatedFilter));
  }

  private void updateInMemoryFilter(SearchTermFilterCombiner<F> searchTermFilterCombiner,
      FilterTester<T, F> filterTester,
      F filter,
      String searchTerm) {
    if (!grid.getDataProvider().isInMemory()) {
      log.warn(
          "Lazy data provider found but expected in memory data provider. Ignoring filter term");
      return;
    }
    F updatedFilter = searchTermFilterCombiner.apply(searchTerm, filter);
    grid.getListDataView().setFilter(it -> filterTester.test(it, updatedFilter));
    fireEvent(new FilterUpdateEvent<>(this.filterType, this, false, filter, updatedFilter));
  }

  /**
   * Add a listener for item count events {@link ItemCountChangeEvent}.
   *
   * @param listener the listener to add
   * @return the registration with which to remove the listener
   * @see com.vaadin.flow.data.provider.LazyDataView#addItemCountChangeListener(ComponentEventListener)
   * @see com.vaadin.flow.data.provider.LazyDataView#addItemCountChangeListener(ComponentEventListener)
   */
  public Registration addItemCountListener(
      ComponentEventListener<ItemCountChangeEvent<FilterGrid<T, F>>> listener) {
    if (grid.getDataProvider().isInMemory()) {
      return grid.getListDataView()
          .addItemCountChangeListener(it -> listener.onComponentEvent(
              new ItemCountChangeEvent<>(this, it.getItemCount(), it.isItemCountEstimated())));
    } else {
      return grid.getLazyDataView()
          .addItemCountChangeListener(it -> listener.onComponentEvent(
              new ItemCountChangeEvent<>(this, it.getItemCount(), it.isItemCountEstimated())));
    }
  }

  /**
   * Gets the last determined item count or fetches the item count if none was present before.
   *
   * @return the assumed number of items.
   */
  public int getItemCount() {
    return grid.getDataCommunicator().getItemCount();
  }

  /**
   * Add a listener for filter update events. {@link FilterUpdateEvent} is thrown when the filter
   * applied to the data is updated by this {@link FilterGrid}.
   *
   * @param listener a component listener listening to a {@link FilterUpdateEvent}
   * @return the registration with which to remove the listener
   */
  public Registration addFilterUpdateListener(
      ComponentEventListener<FilterUpdateEvent<F>> listener) {
    ComponentEventListener componentEventListener = event -> {
      if (!(event instanceof FilterGrid.FilterUpdateEvent<?> filterUpdateEvent)) {
        return; // wrong event type
      }
      if (filterType.isAssignableFrom(filterUpdateEvent.filterType())) {
        var oldFilter = filterUpdateEvent.getOldFilter().map(filterType::cast).orElse(null);
        var updatedFilter = filterType.cast(filterUpdateEvent.updatedFilter);
        listener.onComponentEvent(
            new FilterUpdateEvent<>(filterType, this, event.isFromClient(), oldFilter,
                updatedFilter));
      } else {
        log.debug("Unexpected FilterUpdateEvent. Expected filter type (%s) but got (%s).".formatted(
            this.filterType, filterUpdateEvent.filterType()));
        return; // we do not operate on this event
      }
    };
    return addListener(FilterUpdateEvent.class, componentEventListener);
  }

  /**
   * An update to the applied filter.
   * @param <Y> the type of filter that was updated.
   */
  public static class FilterUpdateEvent<Y> extends ComponentEvent<FilterGrid<?, Y>> {

    private final Y oldFilter;
    private final Y updatedFilter;
    private final Class<Y> filterType;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public FilterUpdateEvent(Class<Y> filterType, FilterGrid<?, Y> source, boolean fromClient,
        Y oldFilter, Y currentFilter) {
      super(source, fromClient);
      this.filterType = Objects.requireNonNull(filterType);
      this.updatedFilter = Objects.requireNonNull(currentFilter);
      this.oldFilter = oldFilter; //can be null
    }

    /**
     * Returns the filter after update.
     * @return the filter after update
     */
    public Y getUpdatedFilter() {
      return filterType.cast(updatedFilter);
    }

    /**
     * Returns the filter to which the update was applied
     * @return
     */
    public Optional<Y> getOldFilter() {
      return Optional.ofNullable(oldFilter);
    }

    Class<Y> filterType() {
      return filterType;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", FilterUpdateEvent.class.getSimpleName() + "[", "]")
          .add("filterType=" + filterType.getSimpleName())
          .add("updatedFilter=" + updatedFilter)
          .toString();
    }
  }

  private @NonNull Div getPrimaryGridControls(List<Column<T>> columns) {
    var primaryGridControls = new Div();
    var spacer = new Div();
    spacer.addClassName("spacer-horizontal-full-width");

    // A vertical line the help separate secondary action group and show/hide button
    var visualSeparator = new Div();
    visualSeparator.addClassNames("border", "border-color-light", "height-07");

    primaryGridControls.addClassNames(FLEX_HORIZONTAL_CSS, GAP_04_CSS, "flex-align-items-center");
    /* Show / Hide Menu */
    MenuBar showHideMenu = new MenuBar();
    var showHideItem = showHideMenu.addItem("Show/Hide Columns");
    var subMenu = showHideItem.getSubMenu();

    CheckboxGroup<Column<T>> checkboxGroup = createCheckboxes(columns);
    preventClickPropagation(checkboxGroup);
    checkboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);

    checkboxGroup.addClassNames("flex-vertical");
    subMenu.add(checkboxGroup);
    primaryGridControls.add(searchField, selectionDisplay, spacer, secondaryActionGroup,
        visualSeparator, showHideMenu);

    return primaryGridControls;
  }

  private @NonNull CheckboxGroup<Column<T>> createCheckboxes(List<Column<T>> columns) {
    CheckboxGroup<Column<T>> checkboxGroup = new CheckboxGroup<>();
    checkboxGroup.setItemLabelGenerator(Column::getHeaderText);

    //state
    checkboxGroup.setItems(columns);
    checkboxGroup.setValue(columns.stream()
        .filter(Component::isVisible
        ).collect(Collectors.toSet()));
    //behaviour
    checkboxGroup.addSelectionListener(event -> {
      for (Column<T> column : columns) {
        column.setVisible(event.getAllSelectedItems().contains(column));
      }
    });
    return checkboxGroup;
  }

  private static <T> void configureGridForMultiSelect(Grid<T> grid) {
    grid.setSelectionMode(SelectionMode.MULTI);
    grid.setSelectionPreservationMode(SelectionPreservationMode.PRESERVE_ALL);
    GridMultiSelectionModel<T> selectionModel = ((GridMultiSelectionModel<T>) grid.getSelectionModel());
    selectionModel.setSelectAllCheckboxVisibility(
        GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE);
    selectionModel.setSelectionColumnFrozen(true);
  }

  private Div createSecondaryActionGroup() {
    final Div actionGroup = new Div();
    actionGroup.addClassNames(FLEX_HORIZONTAL_CSS, GAP_04_CSS);
    return actionGroup;
  }

  private TextField createSearchField() {
    final TextField field;
    field = new TextField();
    field.setSuffixComponent(VaadinIcon.SEARCH.create());
    field.setPlaceholder("Search items");
    field.setClearButtonVisible(true);
    field.addClassName("width-250px");
    field.setValueChangeMode(ValueChangeMode.LAZY);
    return field;
  }

  private static void optimizeGrid(Grid<?> grid, int pageSize) {
    if (grid.getDataProvider().isInMemory()) {
      return;
    }
    var computedPageSize = Math.clamp(pageSize, 100, MAX_QUERY_SIZE);
    grid.setPageSize(computedPageSize);
    // Grid height must not be determined by rows in lazy mode
    grid.setAllRowsVisible(false);
  }

  private static boolean hasContent(String text) {
    return text != null && !text.isBlank();
  }

  /**
   * Defines a custom placeholder for the search field. Default is <i>"Filter"</i>
   * @param placeholder the text to use as placeholder
   * @since 1.12.0
   */
  public void searchFieldPlaceholder(String placeholder) {
    Objects.requireNonNull(placeholder);
    searchField.setPlaceholder(placeholder);
  }

  // helper
  private static void preventClickPropagation(Component c) {
    // prevent the menu-bar from handling the click (which would close the submenu)
    c.getElement().executeJs(
        "this.addEventListener('click', e => e.stopPropagation());");
  }

  private void updateSelectionDisplay(int selectedItemCount) {
    selectionDisplay.removeAll();
    if (selectedItemCount < 1) {
      hideSelectionDisplay();
    } else {
      selectionDisplay.add(
          createSelectionDisplayLabel(currentItemDisplayLabel, selectedItemCount));
      showSelectionDisplay();
    }
  }

  private void showSelectionDisplay() {
    Optional.ofNullable(selectionDisplay).ifPresent(display -> display.setVisible(true));
  }

  private void hideSelectionDisplay() {
    Optional.ofNullable(selectionDisplay).ifPresent(display -> display.setVisible(false));
  }

  private static String formatSelectionDisplayText(String itemLabel, int selectedItemsCount) {
    if (selectedItemsCount <= 1) {
      return "%d %s is selected".formatted(selectedItemsCount, itemLabel);
    }
    return "%d %ss are selected".formatted(selectedItemsCount, itemLabel);
  }

  private static Div createSelectionDisplayLabel(String itemLabel, int selectedItemsCount) {
    Div container = new Div();
    Objects.requireNonNull(itemLabel);
    container.setText(formatSelectionDisplayText(itemLabel, selectedItemsCount));
    return container;
  }

  /**
   * Configures the available secondary action group of the filter grid.
   *
   * @param firstButton the first button from left to right order
   * @param buttons     additional buttons if required for the action group
   * @since 1.12.0
   */
  public void setSecondaryActionGroup(Button firstButton, Button... buttons) {
    Objects.requireNonNull(firstButton);
    Objects.requireNonNull(buttons);
    this.secondaryActionGroup.removeAll();
    this.secondaryActionGroup.add(firstButton);
    for (Button button : buttons) {
      this.secondaryActionGroup.add(button);
    }
  }

  /**
   * Returns the currently selected elements in the grid of type {@code T}.
   *
   * @return a {@link Set} of with the selected elements in the grid of type {@code T}
   * @since 1.12.0
   */
  public @NonNull Set<T> selectedElements() {
    return grid.getSelectedItems();
  }

  /**
   * Returns the assigned class for the filter grid type, so the type of the elements the grid
   * contains.
   *
   * @return the assigned {@link Class} of the filter grid
   * @since 1.12.ß
   */
  public @NonNull Class<T> itemType() {
    return type;
  }

  /**
   * Sets the label for the selected items display.
   * <p>
   * Default is <i>"item"</i>.
   * <p>
   * The plural form is rendered automatically based on the selection count.
   *
   * @param label a concise and contextual description of the elements that can be selected in the
   *              grid
   * @since 1.12.0
   */
  public void itemDisplayLabel(String label) {
    Objects.requireNonNull(label);
    currentItemDisplayLabel = label;
  }

  /**
   * Clears the selection on the grid and deselects all elements.
   *
   * @see Grid#deselectAll()
   */
  public void deselectAll() {
    grid.deselectAll();
  }

  /**
   * Registers a {@link ComponentEventListener} for {@link FilterGridSelectionEvent} to the
   * selection event.
   *
   * @param listener a listener that is called the filter grid fires a
   *                 {@link FilterGridSelectionEvent}.
   * @return a {@link Registration} with the current subscription the client can release again if
   * not needed anymore
   * @since 1.12.0
   */
  public Registration addSelectionListener(
      ComponentEventListener<FilterGridSelectionEvent<T>> listener) {
    return grid.addSelectionListener(it -> listener.onComponentEvent(
        new FilterGridSelectionEvent<>(this, it.getAllSelectedItems(), it.isFromClient())));
  }

  /**
   * A {@link ComponentEvent} that represents a selection happened in the {@link FilterGrid}.
   *
   * @since 1.12.0
   */
  public static final class FilterGridSelectionEvent<T> extends ComponentEvent<FilterGrid<T, ?>> {

    private final transient Set<T> selectedItems;

    /**
     * Creates a new instance of {@link FilterGridSelectionEvent}.
     * <p>
     * The {@code selectedItems} are copied and stored in an unmodifiable collection, so the client
     * is not forced to take extra care to address site effects.
     *
     * @param source        the actual {@link FilterGrid} that emitted the event
     * @param selectedItems the selection items in the source filter grid
     * @param fromClient    if the event is emitted from the client side or not
     * @since 1.12.0
     */
    public FilterGridSelectionEvent(FilterGrid<T, ?> source, Set<T> selectedItems,
        boolean fromClient) {
      super(source, fromClient);
      this.selectedItems = Set.copyOf(Objects.requireNonNull(selectedItems));
    }

    /**
     * Contains the selected elements of type {@code <X>}. The returned {@link Set} is
     * {@code unmodifiable}.
     *
     * @return an {@code unmodifiable} set of the selected items
     * @since 1.12.0
     */
    public Set<T> selectedItems() {
      return selectedItems;
    }
  }

}
