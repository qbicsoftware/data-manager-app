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
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * A {@code FilterGrid} is a UI component that decorates a plain Vaadin {@link Grid} with a consistent
 * "filtering toolbox" and a clear contract for forwarding user-entered filter criteria to the
 * application's data access layer.
 *
 * <h2>When this component matters in the web application</h2>
 * {@code FilterGrid} becomes valuable whenever a view displays a potentially large list of entities
 * and users need to <em>quickly narrow down</em> what they see:
 * <ul>
 *   <li>searching by a free-text term (e.g. name, identifier, description)</li>
 *   <li>keeping selections while paging/refreshing</li>
 *   <li>toggling column visibility to focus on relevant attributes</li>
 *   <li>triggering contextual actions for the currently selected items</li>
 * </ul>
 * The net benefit for users is faster navigation and fewer clicks: the list adapts immediately to
 * the query, and selections/actions remain visible and actionable.
 *
 * <h2>How filtering is forwarded to the Service API / database I/O layer</h2>
 * The search field value is treated as part of the filter model, not as a purely local UI concern.
 * Whenever the user changes the search term, the component combines that term with an existing
 * filter instance and applies it to the underlying data provider.
 *
 * <p>There are two supported integration modes:</p>
 * <ol>
 *   <li>
 *     <b>Lazy (recommended for database-backed lists) via {@link life.qbic.datamanager.views.general.grid.component.GridFilterStrategyFactory.LazyStrategy}</b>
 *     <ul>
 *       <li>The grid uses a {@link ConfigurableFilterDataProvider}.</li>
 *       <li>On search input changes, the component calls {@link SearchTermFilterCombiner} to create an
 *       updated filter and invokes {@code dataProvider.setFilter(updatedFilter)}.</li>
 *       <li>Vaadin will then call your {@link FetchCallback} / {@link CountCallback} with that filter,
 *       which is where your service/database query must interpret the search term.</li>
 *     </ul>
 *     This is the path that "passes down to the database I/O layer": the filter object you define
 *     should contain the search term (and any additional criteria), and your service/repository uses
 *     it to build the query.
 *   </li>
 *   <li>
 *     <b>In-memory (for small datasets)</b> via {@link life.qbic.datamanager.views.general.grid.component.GridFilterStrategyFactory.InMemoryStrategy}:
 *     <ul>
 *       <li>The grid is backed by a list.</li>
 *       <li>On search input changes, the component updates the {@link com.vaadin.flow.data.provider.ListDataView}
 *       filter using your {@link life.qbic.datamanager.views.general.grid.component.GridFilterStrategyFactory.FilterTester} predicate.</li>
 *       <li>No database call is involved; the search term is evaluated in the UI layer.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>Developer usage and extension points</h2>
 * To integrate {@code FilterGrid} into a view:
 * <ol>
 *   <li>Create and configure a Vaadin {@link Grid} (columns, renderers, sorting, etc.).</li>
 *   <li>Define a filter type {@code F} that represents all filtering criteria used by your Service API
 *       (including the search term).</li>
 *   <li>Provide a {@link Supplier} that yields a baseline filter (often "no restrictions").</li>
 *   <li>Provide a {@link SearchTermFilterCombiner} that injects/replaces the current search term into
 *       the filter.</li>
 *   <li>For lazy mode: implement {@link FetchCallback} and {@link CountCallback} to translate {@code F}
 *       into service/repository calls.</li>
 * </ol>
 *
 * <p>Whenever the filter changes, the component fires a {@link FilterUpdateEvent}. Views may listen
 * to this event to synchronize external filter widgets (chips, dropdowns, URL parameters) or to
 * persist filter state.</p>
 *
 * @param <T> the type of items displayed in the filter grid
 * @param <F> the type of filter used in the UI and forwarded to the data provider
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

  private final GridFilterStrategy<T, F> gridFilterStrategy;
  private final Div selectionDisplay = new SelectionNotification();
  private final Div secondaryActionGroup = createSecondaryActionGroup();

  // make sure the search field exists
  private final TextField searchField = createSearchField();

  private String currentItemDisplayLabel = DEFAULT_ITEM_DISPLAY_LABEL;


  public static <T, F> FilterGrid<T, F> create(
      Class<T> itemType,
      Class<F> filterType,
      Supplier<F> filterSupplier,
      SearchTermFilterCombiner<F> searchTermFilterCombiner,
      GridFilterStrategy<T, F> gridFilterStrategy) {
    return new FilterGrid<>(itemType, filterType, filterSupplier,
        searchTermFilterCombiner, gridFilterStrategy);
  }


  private FilterGrid(
      Class<T> itemType,
      Class<F> filterType,
      Supplier<F> filterSupplier,
      SearchTermFilterCombiner<F> searchTermFilterUpdater,
      GridFilterStrategy<T, F> gridFilterStrategy) {
    //assign fields
    this.type = Objects.requireNonNull(itemType);
    this.filterType = Objects.requireNonNull(filterType);
    this.gridFilterStrategy = gridFilterStrategy;

    configureGrid(gridFilterStrategy.getGrid());
    //construct filter Grid component
    constructComponent(gridFilterStrategy.getGrid());

    listenToSelection();
    //update the filter
    searchField.addValueChangeListener(
        event -> updateFilter(searchTermFilterUpdater, filterSupplier.get(), event.getValue()));
  }

  /**
   * Refreshes the grid and clears the selection.
   */
  public void refreshAll() {
    this.gridFilterStrategy.getGrid().getDataProvider().refreshAll();
    this.gridFilterStrategy.getGrid().deselectAll();
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


  private void constructComponent(Grid<T> grid) {
    var primaryGridControls = getPrimaryGridControls(grid.getColumns());
    add(primaryGridControls, grid);
    addClassNames("flex-vertical", "gap-03", "height-full", "width-full");

  }

  private void listenToSelection() {
    updateSelectionDisplay(
        this.gridFilterStrategy.getGrid().getSelectionModel().getSelectedItems().size());
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
   * @param filter                  the filter to update
   * @param searchTerm              the search term to use
   */
  private void updateFilter(SearchTermFilterCombiner<F> searchTermFilterCombiner,
      F filter,
      String searchTerm) {
    F updatedFilter = searchTermFilterCombiner.apply(searchTerm, filter);
    gridFilterStrategy.setFilter(updatedFilter);
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
    ComponentEventListener<ItemCountChangeEvent<?>> itemCountComponentListener = it -> listener.onComponentEvent(
        new ItemCountChangeEvent<>(this, it.getItemCount(), it.isItemCountEstimated()));
    return gridFilterStrategy.addItemCountChangeListener(itemCountComponentListener);
  }

  /**
   * Gets the last determined item count or fetches the item count if none was present before.
   *
   * @return the assumed number of items.
   */
  public int getItemCount() {
    return gridFilterStrategy.getGrid().getDataCommunicator().getItemCount();
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
    return gridFilterStrategy.getGrid().getSelectedItems();
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
    gridFilterStrategy.getGrid().deselectAll();
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
    return gridFilterStrategy.getGrid().addSelectionListener(it -> listener.onComponentEvent(
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
