package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;
import life.qbic.datamanager.views.general.grid.Filter;
import life.qbic.datamanager.views.general.grid.FilterUpdater;
import org.springframework.lang.NonNull;

/**
 * A filter grid comes with some improvements for the user and decorates a plain
 * {@link MultiSelectLazyLoadingGrid}.
 * <p>
 * Some of the improvements are:
 *
 * <ul>
 *   <li>search input field - to look-up for certain terms</li>
 *   <li>show/hide column - a menu to toggle column visibility</li>
 *   <li>secondary action group - to set up a contextual actions for the user that are semantically linked to the selected items</li>
 *   <li>selected items display - a visual indicator about how many items are selected</li>
 * </ul>
 *
 * @since 1.12.0
 */
public final class FilterGrid<T> extends Div {

  public static final String FLEX_HORIZONTAL_CSS = "flex-horizontal";
  public static final String GAP_04_CSS = "gap-04";
  private final MultiSelectLazyLoadingGrid<T> grid;
  private final Div selectionDisplay;
  private final Div secondaryActionGroup;

  private static final int DEFAULT_QUERY_SIZE = 150;

  private static final int MAX_QUERY_SIZE = 350;

  private static final String DEFAULT_ITEM_DISPLAY_LABEL = "item";

  private String currentItemDisplayLabel = DEFAULT_ITEM_DISPLAY_LABEL;

  private Filter currentFilter;

  private MenuBar showShideMenu;

  private Class<T> type;

  private final TextField searchField;

  public FilterGrid(
      Class<T> type,
      MultiSelectLazyLoadingGrid<T> grid,
      CallbackDataProvider<T, Filter> callbackDataProvider,
      Filter initialFilter,
      FilterUpdater filterUpdater) {
    this.type = Objects.requireNonNull(type);
    this.grid = Objects.requireNonNull(grid);
    this.currentFilter = Objects.requireNonNull(initialFilter);

    var dataProvider = Objects.requireNonNull(callbackDataProvider).withConfigurableFilter();
    grid.setDataProvider(dataProvider);
    dataProvider.setFilter(currentFilter);

    searchField = new TextField();
    searchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    searchField.setPlaceholder("Search items");
    searchField.addClassName("width-250px");
    searchField.addValueChangeListener(e ->
    {
      currentFilter = filterUpdater.withSearchTerm(currentFilter, e.getValue());
      dataProvider.setFilter(currentFilter);
      dataProvider.refreshAll();
    });
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    searchField.setValueChangeTimeout(250); // Prevents refreshAll() burst during typing

    this.selectionDisplay = new SelectionNotification();
    hideSelectionDisplay();

    this.secondaryActionGroup = new Div();
    secondaryActionGroup.addClassNames(FLEX_HORIZONTAL_CSS, GAP_04_CSS);

    var primaryGridControls = new Div();

    var spacer = new Div();
    spacer.addClassName("spacer-horizontal-full-width");

    // A vertical line the help separate secondary action group and show/hide button
    var visualSeparator = new Div();
    visualSeparator.addClassNames("border", "border-color-light", "height-07");

    primaryGridControls.add(searchField, selectionDisplay, spacer, secondaryActionGroup);
    primaryGridControls.addClassNames(FLEX_HORIZONTAL_CSS, GAP_04_CSS, "flex-align-items-center");

    fireOnSelectedItems();
    registerToSelectedEvent(grid, this);
    registerToColumnVisibilityChanged(this);

    /* Show / Hide Menu */
    showShideMenu = new MenuBar();
    var showHideItem = showShideMenu.addItem("Show/Hide Columns");
    var subMenu = showHideItem.getSubMenu();

    // Create checkboxes and fire column visibility changed event on checkbox tick
    var checkboxes = createCheckboxesFromColumns(grid.getColumns(), this);

    // Create menu items from checkboxes and keep menu open for multiple selection
    checkboxes.forEach(checkbox -> keepMenuOpenOnClick(subMenu.addItem(checkbox)));

    // Make the layout more compact by removing Vaadin's native hidden checkbox
    subMenu.getItems().forEach(item -> item.getElement().getThemeList().add("no-prefix"));

    showShideMenu.addClassNames(FLEX_HORIZONTAL_CSS);

    primaryGridControls.add(visualSeparator, showShideMenu);
    makeColumnsSortable(grid.getColumns());
    optimizeGrid(grid, DEFAULT_QUERY_SIZE);

    add(primaryGridControls, grid);
    addClassNames("flex-vertical", "gap-03");
    setSizeFull();
  }

  private static void optimizeGrid(MultiSelectLazyLoadingGrid<?> grid, int pageSize) {
    var computedPageSize = Math.clamp(pageSize, 100, MAX_QUERY_SIZE);
    grid.setPageSize(computedPageSize);
    // A sensitive count estimate provides faster initial rendering of the grid
    grid.getLazyDataView().setItemCountEstimate(computedPageSize * 10);
    // A sensitive estimate increase value can increase scrolling speed for the user
    grid.getLazyDataView().setItemCountEstimateIncrease(computedPageSize * 2);
    // Grid height must not be determined by rows in lazy mode
    grid.setAllRowsVisible(false);
  }

  private static <X> List<Column<X>> makeColumnsSortable(List<Column<X>> columns) {
    columns.stream()
        .filter(c -> hasContent(c.getHeaderText()))
        .forEach(c -> c.setSortable(true));
    return columns;
  }

  private static <X> List<Checkbox> createCheckboxesFromColumns(@NonNull List<Column<X>> columns,
      FilterGrid<?> eventSource) {
    Objects.requireNonNull(columns);
    return columns.stream()
        .map(Column::getHeaderText)
        .filter(FilterGrid::hasContent)
        .map(columnHeader -> setupColumnVisibilityBox(columnHeader, eventSource))
        .map(FilterGrid::applyDefaultCheckboxSetting)
        .toList();
  }

  private static boolean hasContent(String text) {
    return text != null && !text.isBlank();
  }

  private static Checkbox setupColumnVisibilityBox(String name, FilterGrid<?> eventSource) {
    var checkbox = new Checkbox(name);
    checkbox.addValueChangeListener(
        e -> eventSource.fireEvent(new ColumnVisibilityChanged(eventSource,
            new ColumnVisibilitySetting(e.getSource().getLabel(), e.getSource().getValue()),
            true)));
    return checkbox;
  }

  private static Checkbox applyDefaultCheckboxSetting(Checkbox checkbox) {
    checkbox.setValue(true);
    return checkbox;
  }

  private Registration addColumnVisibilityChangedListener(
      ComponentEventListener<ColumnVisibilityChanged> listener) {
    return addListener(ColumnVisibilityChanged.class, listener);
  }

  private static void registerToColumnVisibilityChanged(FilterGrid<?> grid) {
    grid.addColumnVisibilityChangedListener(event -> {
      grid.onColumnVisibilityChanged(event.setting);
    });
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

  /**
   * A safe downcast of the grid to the wanted type, if the wanted type is indeed assignable from
   * the current {@link FilterGrid} type.
   *
   * @param wanted the wanted target class of the type the filter grid shall be
   * @param <X>    the type of the filter grid
   * @return a type converted version of the filter grid
   * @throws IllegalArgumentException in case the wanted class is not assignable from the filter
   *                                  grid's type.
   * @since 1.12.0
   */
  @SuppressWarnings("unchecked")
  public <X> FilterGrid<X> as(Class<X> wanted) throws IllegalArgumentException {
    if (wanted.isAssignableFrom(type)) {
      return (FilterGrid<X>) this;
    }
    throw new IllegalArgumentException(
        "Grid type %s is not assignable to %s".formatted(type.getName(), wanted.getName()));
  }

  /**
   * Takes a consumer and calls {@link Consumer#accept(Object)} if the {@code wanted} class is
   * assignable from the current filter grid type ({@link FilterGrid#type()}).
   *
   * @param wanted the wanted class to assign the filter grid to and pass to the consumer
   * @param action the consumer action
   * @param <X>    the class type of the instance of interest
   * @return {@code true}, if the assignment was successful and the grid has beed passed to the
   * consumer, else {@code false}
   * @since 1.12.0
   */
  @SuppressWarnings("unchecked")
  public <X> boolean withType(@NonNull Class<X> wanted,
      @NonNull Consumer<? super FilterGrid<X>> action) {
    Objects.requireNonNull(wanted);
    Objects.requireNonNull(action);
    if (wanted.isAssignableFrom(type)) {
      action.accept((FilterGrid<X>) this);
      return true;
    }
    return false;
  }

  /**
   * Takes a function that consumes a filter grid of type {@code X} and returns an optional of type
   * {@code R}.
   * <p>
   * The passed function is only applied to the filter grid, if the passed class {@code wanted} is
   * assignable from the filter grid's type ({@link FilterGrid#type}).
   *
   * @param wanted the class of type {@code X} that the filter grid shall be assignable from to
   *               execute the function
   * @param fn     the function to apply that takes the filter grid as argument
   * @param <X>    the type of the filter grid
   * @param <R>    the type of the returned output
   * @return an optional of type {@code R}
   * @since 1.12.0
   */
  @SuppressWarnings("unchecked")
  public <X, R> java.util.Optional<R> mapType(@NonNull Class<X> wanted,
      @NonNull Function<? super FilterGrid<X>, ? extends R> fn) {
    java.util.Objects.requireNonNull(wanted);
    java.util.Objects.requireNonNull(fn);
    if (wanted.isAssignableFrom(type)) {
      return java.util.Optional.ofNullable(fn.apply((FilterGrid<X>) this));
    }
    return java.util.Optional.empty();
  }

  private record ColumnVisibilitySetting(String column, boolean visible) {

  }

  private static class ColumnVisibilityChanged extends ComponentEvent<FilterGrid<?>> {

    private final ColumnVisibilitySetting setting;

    public ColumnVisibilityChanged(FilterGrid<?> source, ColumnVisibilitySetting setting,
        boolean fromClient) {
      super(source, fromClient);
      this.setting = setting;
    }

    public ColumnVisibilitySetting setting() {
      return setting;
    }
  }

  /**
   * Get a summary of the currently selected items and their type.
   *
   * @return a {@link SelectionSummary} of the currently selected items
   * @since 1.12.0
   */
  public SelectionSummary<T> selectionSummary() {
    return new SelectionSummary<>(type, grid.getSelectedItems());
  }


  private void onColumnVisibilityChanged(ColumnVisibilitySetting setting) {
    grid.getColumns().stream()
        .filter(column -> column.getHeaderText() != null)
        .filter(column -> column.getHeaderText().equals(setting.column()))
        .forEach(tColumn -> tColumn.setVisible(setting.visible()));
  }

  // helper
  private static void keepMenuOpenOnClick(Component c) {
    // prevent the menu-bar from handling the click (which would close the submenu)
    c.getElement().executeJs(
        "this.addEventListener('click', e => e.stopPropagation());");
  }

  private void updateSelectionDisplay(Set<T> selectedItems) {
    selectionDisplay.removeAll();
    if (selectedItems == null || selectedItems.isEmpty()) {
      hideSelectionDisplay();
    } else {
      selectionDisplay.add(
          createSelectionDisplayLabel(currentItemDisplayLabel, selectedItems.size()));
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

  private static void registerToSelectedEvent(MultiSelectLazyLoadingGrid<?> grid,
      FilterGrid<?> filterGrid) {
    Objects.requireNonNull(grid);
    Objects.requireNonNull(filterGrid);
    grid.addSelectedListener(e -> filterGrid.updateSelectionDisplay(e.getSelectedItems()));
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
  public @NonNull Class<T> type() {
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
   * Registers a {@link ComponentEventListener} for {@link FilterGridSelectionEvent} to the
   * selection event.
   *
   * @param listener a listener that is called the filter grid fires a
   *                 {@link FilterGridSelectionEvent}.
   * @return a {@link Registration} with the current subscription the client can release again if
   * not needed anymore
   * @since 1.12.0
   */
  public Registration addSelectListener(ComponentEventListener<FilterGridSelectionEvent> listener) {
    var eventAdapter = new EventListenerAdapter(this, listener);
    return grid.addSelectedListener(eventAdapter);
  }

  private void fireOnSelectedItems() {
    grid.addSelectedListener(
        event -> {
          Objects.requireNonNull(event); // fail early on NP
          fireEvent(new FilterGridSelectionEvent(this, event.getSelectedItems(), true));
        });
  }

  private static final class EventListenerAdapter
      implements ComponentEventListener<MultiSelectLazyLoadingGrid.SelectedEvent> {

    private final FilterGrid<?> filterGrid;
    private final ComponentEventListener<FilterGridSelectionEvent> target;

    public EventListenerAdapter(FilterGrid<?> grid,
        ComponentEventListener<FilterGridSelectionEvent> target) {
      this.filterGrid = grid;
      this.target = target;
    }

    @Override
    public void onComponentEvent(MultiSelectLazyLoadingGrid.SelectedEvent event) {
      var translatedEvent = new FilterGridSelectionEvent(filterGrid,
          Set.copyOf(event.getSelectedItems()),
          event.isFromClient());
      target.onComponentEvent(translatedEvent);
    }
  }

  public record SelectionSummary<T>(Class<T> type, Set<T> selectedSummary) {

    public SelectionSummary {
      Objects.requireNonNull(type);
      Objects.requireNonNull(selectedSummary);
    }
  }

  /**
   * A {@link ComponentEvent} that represents a selection happened in the {@link FilterGrid}.
   *
   * @since 1.12.0
   */
  public static final class FilterGridSelectionEvent extends ComponentEvent<FilterGrid<?>> {

    private final transient Set<Object> selectedItems;

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
    public FilterGridSelectionEvent(FilterGrid<?> source, Set<Object> selectedItems,
        boolean fromClient) {
      super(source, fromClient);
      this.selectedItems = Set.copyOf(Objects.requireNonNull(selectedItems));
    }

    /**
     * Contains the selected elements of type {@code <X>}. The returned {@link Set} is
     * {@code unmodifiable}.
     *
     * @param <X> the type of the item referenced in the event
     * @return an {@code unmodifiable} set of the selected items
     * @since 1.12.0
     */
    @SuppressWarnings("unchecked")
    public <X> Set<X> selectedItems() {
      return (Set<X>) selectedItems;
    }
  }

}
