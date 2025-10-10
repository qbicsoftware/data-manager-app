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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
  public static final String GAP_02_CSS = "gap-02";
  private final MultiSelectLazyLoadingGrid<T> grid;
  private final Div selectionDisplay;
  private final Div secondaryActionGroup;

  private static final String DEFAULT_ITEM_DISPLAY_LABEL = "item";

  private String currentItemDisplayLabel = DEFAULT_ITEM_DISPLAY_LABEL;

  private Filter<T> currentFilter;

  private MenuBar showShideMenu;

  public FilterGrid(MultiSelectLazyLoadingGrid<T> grid,
      CallbackDataProvider<T, Filter<T>> callbackDataProvider,
      Filter<T> initialFilter, FilterUpdater<T> filterUpdater) {
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

    this.secondaryActionGroup = new Div();
    secondaryActionGroup.addClassNames(FLEX_HORIZONTAL_CSS, GAP_02_CSS);

    var primaryGridControls = new Div();
    primaryGridControls.add(textfield, selectionDisplay, secondaryActionGroup);
    primaryGridControls.addClassNames(FLEX_HORIZONTAL_CSS, GAP_02_CSS,
        "justify-content-space-between");

    add(primaryGridControls, grid);

    fireOnSelectedItems();
    registerToSelectedEvent(grid, this);
    registerToColumnVisibilityChanged(this);

    showShideMenu = new MenuBar();
    var showHideItem = showShideMenu.addItem("Show/Hide Columns");
    var subMenu = showHideItem.getSubMenu();

    var layout = new VerticalLayout(
        createCheckboxesFromColumns(grid.getColumns(), this).toArray(new Component[0]));
    keepMenuOpenOnClick(layout);
    subMenu.addItem(layout);
    showShideMenu.addClassNames(FLEX_HORIZONTAL_CSS);

    primaryGridControls.add(showShideMenu);
    makeColumnsSortable(grid.getColumns());

    optimizeGrid(grid);
  }

  private static void optimizeGrid(MultiSelectLazyLoadingGrid<?> grid) {
    grid.setPageSize(25);
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
      selectionDisplay.setVisible(false);
    } else {
      selectionDisplay.add(createSelectionDisplayLabel(currentItemDisplayLabel, selectedItems.size()));
      selectionDisplay.setVisible(true);
    }
  }

  private static String formatSelectionDisplayText(String itemLabel, int selectedItemsCount) {
    if (selectedItemsCount <= 1) {
      return "Currently %d %s is selected.".formatted(selectedItemsCount, itemLabel);
    }
    return "Currently %d %ss are selected.".formatted(selectedItemsCount, itemLabel);
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

  public Set<T> selectedElements() {
    return grid.getSelectedItems();
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
