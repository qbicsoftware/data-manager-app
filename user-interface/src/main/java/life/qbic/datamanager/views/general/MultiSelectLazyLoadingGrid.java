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
    addSelectAllItemsListener();
  }

  private boolean areAllSelected() {
    return getLazyDataView().getItems().toList().size() == selectedItems.size();
  }

  private Checkbox renderCheckbox(T bean) {
    Checkbox box = new Checkbox();
    box.addValueChangeListener(event -> {
      var checkBoxSelectEvent = new CheckBoxSelectedEvent(box, bean, event.isFromClient());
      updateSelectedItem(event.getValue(), bean);
      listeners.forEach(listener -> listener.onComponentEvent(checkBoxSelectEvent));

      if (event.isFromClient()) {
        Boolean allBoxVal = selectAllCheckBox.getValue();
        if (Boolean.TRUE.equals(allBoxVal) && Boolean.FALSE.equals(box.getValue())) {
          selectAllCheckBox.setValue(false);
        } else if (Boolean.FALSE.equals(allBoxVal) && areAllSelected()) {
          selectAllCheckBox.setValue(true);
        }
      }
    });
    /*Necessary to propagate selection of select all checkbox to individual checkbox*/
    selectAllCheckBox.addValueChangeListener(event -> {
      if(event.isFromClient() && box.getElement().getNode().isAttached()) {
        box.setValue(event.getValue());
      }
    });
    /*Necessary to keep items selected even if grid pagination is reloaded*/
    box.setValue(selectedItems.contains(bean));
    return box;
  }

  private void updateSelectedItem(boolean isSelected, T bean) {
    if (isSelected) {
      selectedItems.add(bean);
    } else {
      selectedItems.remove(bean);
    }
  }

  private void addSelectAllItemsListener() {
    selectAllCheckBox.addValueChangeListener(event -> {
      if(event.isFromClient()) {
        if (Boolean.TRUE.equals(event.getValue())) {
          selectedItems.addAll(getLazyDataView().getItems().toList());
        } else {
          selectedItems.clear();
        }
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

  public class CheckBoxSelectedEvent extends ComponentEvent<Checkbox> {

    private final T bean;

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
