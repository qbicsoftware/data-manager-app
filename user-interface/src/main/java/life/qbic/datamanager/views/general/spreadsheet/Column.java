package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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

  private String name;
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
  private boolean hidden;

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

  void hide() {
    this.hidden = true;
  }

  void show() {
    this.hidden = false;
  }

  public boolean isHidden() {
    return hidden;
  }

  /**
   * @return true if the column requires input; false otherwise
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * @return the editor component of this column
   */
  public Optional<Component> getEditorComponent() {
    return Optional.ofNullable(editorComponent);
  }

  /**
   *
   * @return the name of the column
   */
  public String getName() {
    return name;
  }

  /**
   *
   * @return the list of validators applied to a cell that do not need additional context
   */
  public List<SpreadsheetCellValidator<String>> getValidators() {
    return Collections.unmodifiableList(cellValidators);
  }

  /**
   * @return the list of validators applied to a cell that require the row data for validation
   */
  public List<SpreadsheetObjectValidator<T, String>> getObjectValidators() {
    return Collections.unmodifiableList(objectValidators);
  }

  /**
   * @return a list of validators applied to a cell expecting all values of a column as context
   */
  public List<SpreadsheetObjectValidator<List<String>, String>> getColumnValidators() {
    return Collections.unmodifiableList(columnValidators);
  }

  /**
   *
   * @return the style of cells in that column
   */
  public Optional<CellStyle> getCellStyle() {
    return Optional.ofNullable(cellStyle);
  }

  /**
   * adds a validator
   * @param predicate the predicate to test on the cell value
   * @param errorMessage the error message to display in case validation fails
   * @return the modified column
   */
  public Column<T, C> withValidator(Predicate<String> predicate, String errorMessage) {
    cellValidators.add(new SpreadsheetCellValidator<>(predicate, errorMessage));
    return this;
  }

  /**
   * adds a validator
   *
   * @param predicate    the predicate to test on the cell value and the row value
   * @param errorMessage the error message to display in case validation fails
   * @return the modified column
   */
  public Column<T, C> withValidator(BiPredicate<T, String> predicate, String errorMessage) {
    objectValidators.add(new SpreadsheetObjectValidator<>(predicate, errorMessage));
    return this;
  }

  /**
   * adds a validator
   * @param predicate the predicate to test on the cell value and all cell values in this column
   * @param errorMessage the error message to display in case validation fails
   * @return the modified column
   */
  private Column<T, C> withColumnValidator(BiPredicate<List<String>, String> predicate,
      String errorMessage) {
    columnValidators.add(new SpreadsheetObjectValidator<>(predicate, errorMessage));
    return this;
  }

  /**
   * requires distinct/unique values in the column
   *
   * @return the modified column
   */
  public Column<T, C> requireDistinctValues() {
    this.required = true;
    columnValidators.add(0, new SpreadsheetObjectValidator<>(
        (object, value) -> object.stream().filter(it -> nonNull(it)
                && !it.isBlank()
                && it.equals(value))
            .count() <= 1,
        "The column '" + getName() + "' does not allow duplicate values."));
    return this;
  }


  /**
   * Shows a editor component to facilitate value selection
   *
   * @param values        the values to choose from
   * @param toColumnValue the function to be applied to the value to get the column value
   * @param <E>           the type of items to select from
   * @return the modified column
   */
  public <E> Column<T, C> selectFrom(List<E> values, Function<E, C> toColumnValue) {
    return selectFrom(values, toColumnValue,
        getDefaultComponentRenderer(toColumnValue.andThen(columnValueToCellValue))
    );
  }

  /**
   * Shows a editor component to facilitate value selection
   *
   * @param values        the values to choose from
   * @param toColumnValue the function to convert the selected value to column value
   * @param renderer      the render to use to render possible values
   * @param <E>           the type of items to select from
   * @return the modified column
   */
  public <E> Column<T, C> selectFrom(List<E> values, Function<E, C> toColumnValue,
      ComponentRenderer<? extends Component, E> renderer) {
    return selectFrom(ignored -> values, toColumnValue, renderer);
  }

  /**
   * Shows a editor component facilitating value selection
   *
   * @param valueProvider a function taking a row value and providing a list of possible items
   * @param toColumnValue the function to convert an option to a column value
   * @param <E>           the type of items to select from
   * @return the modified column
   */
  public <E> Column<T, C> selectFrom(Function<T, List<E>> valueProvider,
      Function<E, C> toColumnValue) {
    return selectFrom(valueProvider, toColumnValue,
        getDefaultComponentRenderer(toColumnValue.andThen(columnValueToCellValue)));
  }

  /**
   * shows a editor component facilitating value selection
   *
   * @param valueProvider a function taking a row value and providing a list of possible items
   * @param toColumnValue the function to convert an option to a column value
   * @param renderer      a renderer rendering an option
   * @param <E>           the type of items to select from
   * @return the modified column
   */
  public <E> Column<T, C> selectFrom(Function<T, List<E>> valueProvider,
      Function<E, C> toColumnValue,
      ComponentRenderer<? extends Component, E> renderer) {
    Function<E, String> adaptedToCellValue = toColumnValue.andThen(columnValueToCellValue);

    this.withValidator((object, value) -> valueProvider.apply(object)
            .stream()
            .map(adaptedToCellValue)
            .anyMatch(allowedValue -> isNull(allowedValue)
                || allowedValue.isBlank()
                || allowedValue.equals(value)),
        "'{0}' is not a valid option for column '" + getName() + "' in this row.");

    SelectEditor<T, E> selectEditor = new SelectEditor<>(valueProvider, adaptedToCellValue);
    selectEditor.setRenderer(renderer);
    selectEditor.setItemLabelGenerator(adaptedToCellValue::apply);
    this.editorComponent = selectEditor;
    return this;
  }

  /**
   * sets the cell style for cells in this column
   * @param cellStyle the cell style to apply
   * @return the modified column
   */
  public Column<T, C> withCellStyle(CellStyle cellStyle) {
    this.cellStyle = cellStyle;
    return this;
  }

  /**
   * Sets the column to require non-empty input
   * @return the modified column
   */
  public Column<T, C> setRequired() {
    this.required = true;
    this.name = this.name + "*";
    cellValidators.add(0, new SpreadsheetCellValidator<>(
        object -> (Objects.nonNull(object) && !object.isBlank()) || !this.isRequired(),
        "The column '" + getName() + "' does not allow empty values.\nPlease enter a value."));
    return this;
  }

  /**
   * the model updater
   * @return a bi-consumer applied on the model with a cell value for cells in this column
   */
  BiConsumer<T, String> modelUpdater() {
    return modelUpdater;
  }

  /**
   * converts row values to the cell values in this column
   * @param t the row data to convert
   * @return the cell value
   */
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
