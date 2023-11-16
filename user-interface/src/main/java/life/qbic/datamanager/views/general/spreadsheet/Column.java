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
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import life.qbic.datamanager.views.general.spreadsheet.validation.SpreadsheetCellValidator;
import life.qbic.datamanager.views.general.spreadsheet.validation.SpreadsheetObjectValidator;
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
public class Column<T, C> {

  private final String name;
  private final List<SpreadsheetCellValidator<String>> cellValidators;
  private final List<SpreadsheetObjectValidator<T, String>> objectValidators;

  private final List<SpreadsheetObjectValidator<List<String>, String>> columnValidators;
  private final Function<T, String> toCellValue;

  private final Function<T, C> toColumnValue;
  private final Function<C, String> columnValueToCellValue;
  private final BiConsumer<T, String> modelUpdater;
  private CellStyle cellStyle;
  private Component editorComponent;
  private boolean required;

//  public Column(String name, Function<T, String> toCellValue,
//      BiConsumer<T, String> modelUpdater) {
//    requireNonNull(name, "name must not be null");
//    requireNonNull(toCellValue, "toCellValue must not be null");
//    requireNonNull(modelUpdater, "modelUpdater must not be null");
//    this.name = name;
//    this.toCellValue = toCellValue;
//    this.modelUpdater = modelUpdater;
//    this.editorComponent = null;
//    this.required = false;
//    this.cellValidators = new ArrayList<>();
//    toColumnValue = t -> null; //FIXME
//    columnValueToCellValue = c -> null; //FIXME
//  }

  public Column(String name, Function<T, C> toColumnValue,
      Function<C, String> columnValueToCellValue,
      BiConsumer<T, String> modelUpdater) {
    requireNonNull(name, "name must not be null");
    requireNonNull(toColumnValue, "toColumnValue must not be null");
    requireNonNull(columnValueToCellValue, "columnValueToCellValue must not be null");
    requireNonNull(modelUpdater, "modelUpdater must not be null");
    this.name = name;
    this.toColumnValue = toColumnValue;
    this.columnValueToCellValue = columnValueToCellValue;
    this.toCellValue = toColumnValue.andThen(columnValueToCellValue);
    this.modelUpdater = modelUpdater;
    editorComponent = null;
    required = false;
    cellValidators = new ArrayList<>();
    objectValidators = new ArrayList<>();
    columnValidators = new ArrayList<>();
  }

  public boolean isRequired() {
    return required;
  }

  public Optional<Component> getEditorComponent() { //FIXME if desired re-generate the editor component
    return Optional.ofNullable(editorComponent);
  }

  public String getName() {
    return name;
  }

  public List<SpreadsheetCellValidator<String>> getValidators() {
    return Collections.unmodifiableList(cellValidators);
  }

  public List<SpreadsheetObjectValidator<T, String>> getObjectValidators() {
    return Collections.unmodifiableList(objectValidators);
  }

  public List<SpreadsheetObjectValidator<List<String>, String>> getColumnValidators() {
    return Collections.unmodifiableList(columnValidators);
  }

  public Optional<CellStyle> getCellStyle() {
    return Optional.ofNullable(cellStyle);
  }

  public Column<T, C> withValidator(Predicate<String> predicate, String errorMessage) {
    cellValidators.add(new SpreadsheetCellValidator<>(predicate, errorMessage));
    return this;
  }

  public Column<T, C> withValidator(BiPredicate<T, String> predicate, String errorMessage) {
    objectValidators.add(new SpreadsheetObjectValidator<>(predicate, errorMessage));
    return this;
  }

  private Column<T, C> withColumnValidator(BiPredicate<List<String>, String> predicate,
      String errorMessage) {
    columnValidators.add(new SpreadsheetObjectValidator<>(predicate, errorMessage));
    return this;
  }

  public Column<T, C> requireDistinctValues() {
    this.required = true;
    columnValidators.add(0, new SpreadsheetObjectValidator<>(
        (object, value) -> object.stream().filter(it -> it.equals(value)).count() <= 1,
        "The column '" + getName() + "' does not allow duplicate values."));
    return this;
  }


  public <E> Column<T, C> selectFrom(List<E> values, Function<E, C> toColumnValue) {
    return selectFrom(values, toColumnValue,
        getDefaultComponentRenderer(toColumnValue.andThen(columnValueToCellValue)),
        (e, t) -> {/*ignored*/});
  }

  public <E> Column<T, C> selectFrom(List<E> values, Function<E, C> toColumnValue,
      ComponentRenderer<? extends Component, E> renderer) {
    return selectFrom(ignored -> values, toColumnValue, renderer, (e, t) -> {/* ignored */});
  }

  public <E> Column<T, C> selectFrom(List<E> values, Function<E, C> toColumnValue,
      BiConsumer<E, T> modelUpdater) {
    return selectFrom(values, toColumnValue,
        getDefaultComponentRenderer(toColumnValue.andThen(columnValueToCellValue)), modelUpdater);
  }

  public <E> Column<T, C> selectFrom(List<E> values, Function<E, C> toColumnValue,
      ComponentRenderer<? extends Component, E> renderer, BiConsumer<E, T> modelUpdater) {
    return selectFrom(ignored -> values, toColumnValue, renderer, modelUpdater);
  }

  public <E> Column<T, C> selectFrom(Function<T, List<E>> valueProvider,
      Function<E, C> toColumnValue,
      ComponentRenderer<? extends Component, E> renderer,
      BiConsumer<E, T> modelUpdater) {
    Function<E, String> adaptedToCellValue = toColumnValue.andThen(columnValueToCellValue);

    this.withValidator((object, value) -> valueProvider.apply(object)
            .stream()
            .map(adaptedToCellValue)
            .anyMatch(allowedValue -> isNull(allowedValue)
                || allowedValue.isBlank()
                || allowedValue.equals(value)),
        "'{0}' is not a valid option for column '" + getName() + "'.");

    SelectEditor<T, E> selectEditor = new SelectEditor<>(valueProvider, adaptedToCellValue,
        modelUpdater);
    selectEditor.setRenderer(renderer);
    selectEditor.setItemLabelGenerator(adaptedToCellValue::apply);
    this.editorComponent = selectEditor;
    return this;
  }

  public Column<T, C> withCellStyle(CellStyle cellStyle) {
    this.cellStyle = cellStyle;
    return this;
  }

  public Column<T, C> setRequired() {
    this.required = true;
    cellValidators.add(0, new SpreadsheetCellValidator<>(
        object -> (Objects.nonNull(object) && !object.isBlank()) || !this.isRequired(),
        "The column '" + getName() + "' does not allow empty values.\nPlease enter a value."));
    return this;
  }

  BiConsumer<T, String> modelUpdater() {
    return modelUpdater;
  }

  String toCellValue(T t) {
    if (isNull(t)) {
      return null;
    }
    if (isNull(toColumnValue.apply(t))) {
      return null;
    }
    return toCellValue.apply(t);
  }

  private static <E> ComponentRenderer<Component, E> getDefaultComponentRenderer(
      Function<E, String> toCellValue) {
    return new ComponentRenderer<>(item -> {
      Span listItem = new Span(toCellValue.apply(item));
      listItem.addClassName("spreadsheet-list-item");
      return listItem;
    });
  }

}
