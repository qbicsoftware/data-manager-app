package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Grid that allows lazy loading and the use of a selection column.
 * <p>
 * Vaadin does not natively allow the use of a selection column (and select-all), as the concept of
 * "all" is not clearly defined with lazy loading and there are problems with keeping items in the
 * grid selected when scrolling. This component solves these issues. Using the "select all" box
 * selects all items contained in the LazyDataView. These are all filtered items, irrespective of
 * limit and offset.
 *
 * @since 1.0.0
 */
public class MultiSelectLazyLoadingGrid<T> extends Grid<T> {

  private final Column<T> selectionColumn;

  private final Set<T> selectedItems = new HashSet<>();

  private final List<ComponentEventListener<CheckBoxSelectedEvent>> listeners = new ArrayList<>();

  private final Checkbox selectAllCheckBox = new Checkbox();

  public MultiSelectLazyLoadingGrid() {
    selectionColumn = addComponentColumn(this::renderCheckbox).setHeader(selectAllCheckBox);
    selectionColumn.setFlexGrow(0);
    addSelectedListener();
  }

  private boolean areAllSelected() {
    return getLazyDataView().getItems().toList().size() == selectedItems.size();
  }

  private Checkbox renderCheckbox(T bean) {
    Checkbox box = new Checkbox();
    Registration boxListener = box.addValueChangeListener(event -> {
      var checkBoxSelectEvent = new CheckBoxSelectedEvent(box, bean, event.isFromClient());
      updateSelectedItem(event.getValue(), bean);
      listeners.forEach(listener -> listener.onComponentEvent(checkBoxSelectEvent));
      // if a user selected/deselected one box, we check if the select all checkbox needs to change
      if (event.isFromClient()) {
        fireEvent(new SelectedEvent(this, getSelectedItems(), event.isFromClient()));
        updateAllCheckBox(event.getValue());
      }
    });
    /*Necessary to propagate selection of select all checkbox to individual checkbox*/
    Registration allSelectBoxListener = selectAllCheckBox.addValueChangeListener(event -> {
      if (event.isFromClient()) {
        box.setValue(event.getValue());
      }
    });
    /*Necessary to keep items selected even if grid pagination is reloaded*/
    box.setValue(selectedItems.contains(bean));

    //if the box is removed, we remove the respective listeners
    box.addDetachListener(detachEvent -> {
      boxListener.remove();
      allSelectBoxListener.remove();
    });

    return box;
  }

  private void updateAllCheckBox(Boolean value) {
    Boolean allBoxVal = selectAllCheckBox.getValue();
    // all boxes were selected -> user deselected one
    if (Boolean.TRUE.equals(allBoxVal) && Boolean.FALSE.equals(value)) {
      selectAllCheckBox.setValue(false);
    }
    // all but one box were selected -> all are now selected
    else if (Boolean.FALSE.equals(allBoxVal) && areAllSelected()) {
      selectAllCheckBox.setValue(true);
    }
  }

  private void updateSelectedItem(boolean isSelected, T bean) {
    if (isSelected) {
      selectedItems.add(bean);
    } else {
      selectedItems.remove(bean);
    }
  }

  private void addSelectedListener() {
    selectAllCheckBox.addValueChangeListener(event -> {
      if (event.isFromClient()) {
        if (Boolean.TRUE.equals(event.getValue())) {
          selectedItems.addAll(getLazyDataView().getItems().toList());
        } else {
          selectedItems.clear();
        }
        fireEvent(new SelectedEvent(this, getSelectedItems(), event.isFromClient()));
      }
      var checkBoxSelectEvent = new CheckBoxSelectedEvent(selectAllCheckBox, null,
          event.isFromClient());
      listeners.forEach(listener -> listener.onComponentEvent(checkBoxSelectEvent));
    });
  }

  @Override
  public Set<T> getSelectedItems() {
    return selectedItems;
  }

  public void clearSelectedItems() {
    selectAllCheckBox.setValue(false);
    selectedItems.clear();
  }

  public void setSelectionColumnVisible(boolean isVisible) {
    selectionColumn.setVisible(isVisible);
  }

  @Override
  public Registration addSelectionListener(SelectionListener<Grid<T>, T> listener) {
    throw new IllegalStateException("This Grid has a hardcoded selection model");
  }

  public void addSelectListener(ComponentEventListener<CheckBoxSelectedEvent> listener) {
    listeners.add(listener);
  }

  /**
   * Adds a listener to the event {@link SelectedEvent}.
   *
   * <p>The {@link ComponentEventListener#onComponentEvent} is invoked in the following and only
   * following
   * cases:
   *
   * <ul>
   *   <li>the client selected the {@code select all} checkbox</li>
   *   <li>the client selected an individual checkbox</li>
   * </ul>
   *
   * @param listener a listener that gets informed in case an {@link SelectedEvent} is fired
   * @return a registration handle to cancel the registration again
   * @since 1.12.0
   */
  public Registration addSelectedListener(ComponentEventListener<SelectedEvent> listener) {
    return addListener(SelectedEvent.class, listener);
  }

  public static class SelectedEvent extends ComponentEvent<MultiSelectLazyLoadingGrid<?>> {

    private final Set<?> selectedItems;

    public SelectedEvent(MultiSelectLazyLoadingGrid<?> source, Set<?> selectedItems,
        boolean fromClient) {
      super(source, fromClient);
      this.selectedItems = Set.copyOf(Objects.requireNonNull(selectedItems));
    }

    @SuppressWarnings("unchecked")
    public <X> Set<X> getSelectedItems() {
      return (Set<X>) selectedItems;
    }
  }

  public class CheckBoxSelectedEvent extends ComponentEvent<Checkbox> {

    private final transient T bean;

    private final boolean isSelected;

    public CheckBoxSelectedEvent(Checkbox checkbox, T bean, boolean fromClient) {
      super(checkbox, fromClient);
      this.isSelected = checkbox.getValue();
      this.bean = bean;
    }

    public T getBean() {
      return bean;
    }

    public boolean isSelected() {
      return isSelected;
    }
  }
}
