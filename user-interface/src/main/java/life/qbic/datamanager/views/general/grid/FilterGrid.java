package life.qbic.datamanager.views.general.grid;

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
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;
import life.qbic.datamanager.views.general.grid.component.SelectionNotification;
import org.springframework.lang.NonNull;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FilterGrid<T> extends Div {

  public static final String FLEX_HORIZONTAL_CSS = "flex-horizontal";
  public static final String GAP_04_CSS = "gap-04";
  private final MultiSelectLazyLoadingGrid<T> grid;
  private final Div selectionDisplay;
  private final Div secondaryActionGroup;

  private static final int DEFAULT_QUERY_SIZE = 150;

  private static final int MAX_QUERY_SIZE = 350;

  private static final String DEFAULT_ITEM_DISPLAY_LABEL = "item";

  private String currentItemDisplayLabel = DEFAULT_ITEM_DISPLAY_LABEL;

  private Filter<T> currentFilter;

  private MenuBar showShideMenu;

  private Class<T> type;

  public FilterGrid(
      Class<T> type,
      MultiSelectLazyLoadingGrid<T> grid,
      CallbackDataProvider<T, Filter<T>> callbackDataProvider,
      Filter<T> initialFilter,
      FilterUpdater<T> filterUpdater) {
    this.type = Objects.requireNonNull(type);
    this.grid = Objects.requireNonNull(grid);
    this.currentFilter = Objects.requireNonNull(initialFilter);

    var dataProvider = Objects.requireNonNull(callbackDataProvider).withConfigurableFilter();
    grid.setDataProvider(dataProvider);
    dataProvider.setFilter(currentFilter);

    var textfield = new TextField();
    textfield.setSuffixComponent(VaadinIcon.SEARCH.create());
    textfield.setPlaceholder("Filter");
    textfield.addValueChangeListener(e ->
    {
      currentFilter = filterUpdater.withSearchTerm(currentFilter, e.getValue());
      dataProvider.setFilter(currentFilter);
      dataProvider.refreshAll();
    });
    textfield.setValueChangeMode(ValueChangeMode.EAGER);

    this.selectionDisplay = new SelectionNotification();
    hideSelectionDisplay();

    this.secondaryActionGroup = new Div();
    secondaryActionGroup.addClassNames(FLEX_HORIZONTAL_CSS, GAP_04_CSS);

    var primaryGridControls = new Div();

    var spacer = new Div();
    spacer.addClassName("spacer-horizontal-full-width");

    var visualSeparator = new Div();
    visualSeparator.addClassNames("border", "border-color-light", "height-07");

    primaryGridControls.add(textfield, selectionDisplay, spacer, secondaryActionGroup);
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
    var computedPageSize = Math.clamp(pageSize, 100 , MAX_QUERY_SIZE);
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

  /** Safe downcast after runtime check. */
  @SuppressWarnings("unchecked")
  public <X> FilterGrid<X> as(Class<X> wanted) {
    if (wanted.isAssignableFrom(type)) {
      return (FilterGrid<X>) this;
    }
    throw new IllegalArgumentException(
        "Grid type %s is not assignable to %s".formatted(type.getName(), wanted.getName()));
  }

  @SuppressWarnings("unchecked")
  public <X> boolean withType(Class<X> wanted, Consumer<? super FilterGrid<X>> action) {
    Objects.requireNonNull(wanted);
    Objects.requireNonNull(action);
    if (wanted.isAssignableFrom(type)) {
      action.accept((FilterGrid<X>) this);
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public <X, R> java.util.Optional<R> mapType(Class<X> wanted,
      java.util.function.Function<? super FilterGrid<X>, ? extends R> fn) {
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

  public void setSecondaryActionGroup(Button firstButton, Button... buttons) {
    Objects.requireNonNull(firstButton);
    Objects.requireNonNull(buttons);
    this.secondaryActionGroup.removeAll();
    this.secondaryActionGroup.add(firstButton);
    for (Button button : buttons) {
      this.secondaryActionGroup.add(button);
    }
  }

  public @NonNull Set<T> selectedElements() {
    return grid.getSelectedItems();
  }

  public @NonNull Class<T> type() {
    return type;
  }

  public void itemDisplayLabel(String label) {
    Objects.requireNonNull(label);
    currentItemDisplayLabel = label;
  }

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
