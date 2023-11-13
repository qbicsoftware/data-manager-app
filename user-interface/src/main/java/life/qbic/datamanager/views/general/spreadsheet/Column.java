package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.poi.ss.usermodel.CellStyle;

/**
 * A spreadsheet column.
 * <p/>
 * The spreadsheet column provides information about how the content of a column in the spreadsheet
 * is handeled. Next to custom validation logice, it can provide a custom editor component as well
 * as a column name.
 * <p/>
 * Columns themselves do not hold any data. They act only in describing data in a specific column,
 * how it can be derived from a bean and how the bean will be modified when the value in cells
 * within the column changes.
 */
public class Column<T> {

  private final String name;
  private final List<ColumnValidator<String>> validators;

  private final Function<T, String> toCellValue;
  private final BiConsumer<T, String> modelUpdater;

  private CellStyle cellStyle;
  private Component editorComponent;
  private boolean required;

  public Column(String name, Function<T, String> toCellValue,
      BiConsumer<T, String> modelUpdater) {
    requireNonNull(name, "name must not be null");
    requireNonNull(toCellValue, "toCellValue must not be null");
    requireNonNull(modelUpdater, "modelUpdater must not be null");
    this.name = name;
    this.toCellValue = toCellValue;
    this.modelUpdater = modelUpdater;
    editorComponent = null;
    required = false;
    validators = new ArrayList<>();
  }

  public boolean isRequired() {
    return required;
  }

  public Optional<Component> getEditorComponent() {
    return Optional.ofNullable(editorComponent);
  }

  public String getName() {
    return name;
  }

  public List<ColumnValidator<String>> getValidators() {
    return Collections.unmodifiableList(validators);
  }

  public Optional<CellStyle> getCellStyle() {
    return Optional.ofNullable(cellStyle);
  }

  public Column<T> withValidator(Predicate<String> predicate, String errorMessage) {
    validators.add(new ColumnValidator<>(predicate, errorMessage));
    return this;
  }

  public <E> Column<T> selectFrom(List<E> values, Function<E, String> toCellValue) {
    return selectFrom(values, toCellValue, getDefaultComponentRenderer());
  }

  public <E> Column<T> selectFrom(List<E> values, Function<E, String> toCellValue,
      ComponentRenderer<? extends Component, E> renderer) {
    List<String> possibleCellValues = values.stream()
        .map(toCellValue).toList();
    this.withValidator(value -> isNull(value) || value.isBlank()
            || possibleCellValues.stream().anyMatch(it -> it.equals(value)),
        "'{0}' is not a valid option for column %s. Please choose from %s".formatted(getName(),
            possibleCellValues));
    SelectEditor<E> selectEditor = new SelectEditor<>(values, toCellValue);
    selectEditor.setRenderer(renderer);
    selectEditor.setItemLabelGenerator(toCellValue::apply);
    this.editorComponent = selectEditor;
    return this;
  }

  public Column<T> withCellStyle(CellStyle cellStyle) {
    this.cellStyle = cellStyle;
    return this;
  }

  public Column<T> setRequired() {
    this.required = true;
    validators.add(0, new ColumnValidator<>(
        object -> (Objects.nonNull(object) && !object.isBlank()) || !this.isRequired(),
        "The column '" + getName() + "' does not allow empty values. Please enter a value."));
    return this;
  }

  BiConsumer<T, String> modelUpdater() {
    return modelUpdater;
  }

  String toCellValue(T t) {
    return toCellValue.apply(t);
  }

  private static <E> ComponentRenderer<Component, E> getDefaultComponentRenderer() {
    return new ComponentRenderer<>(item -> {
      Span listItem = new Span(item.toString());
      listItem.addClassName("spreadsheet-list-item");
      return listItem;
    });
  }

}
