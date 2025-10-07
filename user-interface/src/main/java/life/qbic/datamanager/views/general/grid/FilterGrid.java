package life.qbic.datamanager.views.general.grid;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.Objects;
import java.util.Set;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FilterGrid<T> extends Div {

  private final MultiSelectLazyLoadingGrid<T> grid;
  private final Div selectionDisplay;
  private final Div secondaryActionGroup;

  private Filter<T> currentFilter;

  public FilterGrid(MultiSelectLazyLoadingGrid<T> grid,
      CallbackDataProvider<T, Filter<T>> callbackDataProvider,
      Filter<T> initialFilter, FilterUpdater<T> filterUpdater) {
    this.grid = Objects.requireNonNull(grid);
    this.currentFilter = Objects.requireNonNull(initialFilter);

    var dataProvider = Objects.requireNonNull(callbackDataProvider).withConfigurableFilter();
    grid.setDataProvider(dataProvider);
    dataProvider.setFilter(currentFilter);

    var textfield = new TextField();
    textfield.setPlaceholder("Filter");
    textfield.addValueChangeListener(e ->
    {
      currentFilter = filterUpdater.withSearchTerm(currentFilter, e.getValue());
      dataProvider.setFilter(currentFilter);
      dataProvider.refreshAll();
    });
    textfield.setValueChangeMode(ValueChangeMode.EAGER);

    this.selectionDisplay = new Div();

    this.secondaryActionGroup = new Div();
    secondaryActionGroup.addClassNames("flex-horizontal", "gap-02");

    var primaryGridControls = new Div();
    primaryGridControls.add(textfield,  selectionDisplay, secondaryActionGroup);
    primaryGridControls.addClassNames("flex-horizontal", "gap-02", "justify-content-space-between");

    add(primaryGridControls, grid);
    fireOnSelectedItems();
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
      var translatedEvent = new FilterGridSelectionEvent(filterGrid, Set.copyOf(event.getSelectedItems()),
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
