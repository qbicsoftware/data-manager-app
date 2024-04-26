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
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class MultiSelectLazyLoadingGrid<T> extends Grid<T> {

  private final Column<T> selectionColumn;

  private final Set<T> selectedItems = new HashSet<>();

  private final List<ComponentEventListener<CheckBoxSelectedEvent>> listeners = new ArrayList<>();

  private final Checkbox selectAllCheckBox = new Checkbox();

  /**
   * Creates a new instance, with page size of 50.
   */
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
        if(selectAllCheckBox.getValue() && !box.getValue()) {
          selectAllCheckBox.setValue(false);
        } else if(!selectAllCheckBox.getValue() && areAllSelected()) {
          System.err.println("all should be selected");
          selectAllCheckBox.setValue(true);
        }
      }
    });
    /*Necessary to propagate selection of select all checkbox to individual checkbox*/
    selectAllCheckBox.addValueChangeListener(event -> {
      if(event.isFromClient()) {
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
        if (event.getValue()) {
          selectedItems.addAll(getLazyDataView().getItems().toList());
        } else {
          getLazyDataView().getItems().toList().forEach(selectedItems::remove);
        }
      }
      //Todo Should this be treated differently?
      var checkBoxSelectEvent = new CheckBoxSelectedEvent(selectAllCheckBox, null,
          event.isFromClient());
      listeners.forEach(listener -> listener.onComponentEvent(checkBoxSelectEvent));
    });
  }

  public Set<T> getSelectedItems() {
    return selectedItems;
  }

  public void clearSelectedItems() {
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
