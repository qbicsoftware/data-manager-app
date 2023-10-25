package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.isNull;

import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A cell value editor enabling the user to make a selection.
 */
class SelectEditor<E> extends Select<E> {

  private final List<Registration> addedValueChangeListeners;

  private final Function<E, String> toCellValue;

  public SelectEditor(List<E> items, Function<E, String> toCellValue) {
    addedValueChangeListeners = new ArrayList<>();
    setItems(items);
    this.toCellValue = toCellValue;
    addValueChangeListener(event -> {

    });
  }

  public String toCellValue(E value) {
    if (isNull(value)) {
      return null;
    }
    return toCellValue.apply(value);
  }

  public void setFromCellValue(String cellValue) {
    getListDataView().getItems()
        .filter(it -> toCellValue.apply(it).equals(cellValue))
        .findFirst()
        .ifPresentOrElse(this::setValue, this::clear);
  }

  @Override
  public Registration addValueChangeListener(
      ValueChangeListener<? super ComponentValueChangeEvent<Select<E>, E>> listener) {
    Registration registration = super.addValueChangeListener(listener);
    // as addedValueChangeListeners is final, it is not null when called from this class
    if (addedValueChangeListeners == null) {
      //vaadin calls this method in the super constructor. Ignore those.
      return registration;
    }
    addedValueChangeListeners.add(registration);
    return registration;
  }

  public void removeAllValueChangeListeners() {
    addedValueChangeListeners.forEach(Registration::remove);
    addedValueChangeListeners.clear();
  }
}
