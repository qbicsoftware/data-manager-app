package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.spreadsheet.Spreadsheet.CellValueChangeEvent;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.ColumnValidator.ValidationResult;
import life.qbic.logging.api.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Tag(Tag.DIV)
public final class Spreadsheet<T> extends Component implements HasComponents,
    HasValidationProperties {

  private static final Logger log = logger(Spreadsheet.class);

  private final com.vaadin.flow.component.spreadsheet.Spreadsheet delegateSpreadsheet = new com.vaadin.flow.component.spreadsheet.Spreadsheet();
  private final List<Column<T>> columns = new ArrayList<>();
  private final List<Row> rows = new ArrayList<>();

  // cell styles
  private final CellStyle defaultCellStyle;
  private final CellStyle invalidCellStyle;
  private final CellStyle rowNumberStyle;
  private final CellStyle columnHeaderStyle;

  // apache helpers
  private final CreationHelper creationHelper;
  private final Drawing<?> drawingPatriarch;

  private ValidationMode validationMode;

  public enum ValidationMode {
    LAZY,
    EAGER
  }

  public Spreadsheet() {
    addClassName("spreadsheet-container");
    delegateSpreadsheet.setActiveSheetProtected("");
    creationHelper = delegateSpreadsheet.getWorkbook().getCreationHelper();
    drawingPatriarch = delegateSpreadsheet.getActiveSheet().createDrawingPatriarch();

    defaultCellStyle = getDefaultCellStyle(delegateSpreadsheet.getWorkbook());
    invalidCellStyle = createInvalidCellStyle(delegateSpreadsheet.getWorkbook());
    rowNumberStyle = createRowNumberStyle(delegateSpreadsheet.getWorkbook());
    columnHeaderStyle = createColumnNameStyle(delegateSpreadsheet.getWorkbook());

    delegateSpreadsheet.setSheetSelectionBarVisible(false);
    delegateSpreadsheet.setFunctionBarVisible(false);
    delegateSpreadsheet.setRowColHeadingsVisible(false);
    delegateSpreadsheet.addCellValueChangeListener(this::onCellValueChanged);
    delegateSpreadsheet.setSizeFull();
    delegateSpreadsheet.setSpreadsheetComponentFactory(new MyComponentFactory());
    setErrorMessage("Please complete the missing mandatory information.");

    delegateSpreadsheet.setMaxRows(rowCount());
    add(delegateSpreadsheet);

    validationMode = ValidationMode.LAZY;
    Column<T> rowNumberColumn = addColumn("",
        rowValue -> String.valueOf(dataRowCount()),
        (rowValue, cellValue) -> {/* do nothing */})
        .withCellStyle(rowNumberStyle);
    addHeaderRow();
  }

  public void validate() {
    List<Cell> cells = cells();
    updateValidation(cells);
    delegateSpreadsheet.refreshCells(cells);
  }

  public void addRow(T rowData) {
    int previousRowCount = rowCount();
    var dataRow = new DataRow(rowData);
    rows.add(dataRow);
    createCellsForRow(dataRow);
    delegateSpreadsheet.setMaxRows(previousRowCount + 1);
  }

  private void addHeaderRow() {
    int previousRowCount = rowCount();
    var headerRow = new HeaderRow();
    rows.add(headerRow);
    createCellsForRow(headerRow);
    delegateSpreadsheet.setMaxRows(previousRowCount + 1);
  }

  public Column<T> addColumn(String name, Function<T, String> toCellValue,
      BiConsumer<T, String> modelEditor) {
    Column<T> column = new Column<>(name, toCellValue, modelEditor);
    columns.add(column);
    delegateSpreadsheet.refreshCells(createCellsForColumn(column));
    delegateSpreadsheet.setMaxColumns(columnCount());
    return column;
  }

  public void removeLastRow() {
    if (rowCount() == 0) {
      return;
    }
    deleteRow(rowCount() - 1);
  }

  public void setValidationMode(ValidationMode validationMode) {
    this.validationMode = validationMode;
  }

  public List<T> getData() {
    return rows.stream()
        .filter(row -> row instanceof DataRow)
        .map(row -> (DataRow) row)
        .map(DataRow::data)
        .toList();
  }

  public boolean isValid() {
    return !isInvalid();
  }

  private CellStyle createColumnNameStyle(Workbook workbook) {
    Font columnNameFont = workbook.createFont();
    columnNameFont.setBold(true);

    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setFillBackgroundColor(null);
    cellStyle.setFont(columnNameFont);
    cellStyle.setAlignment(HorizontalAlignment.CENTER);

    cellStyle.setLocked(true);
    return cellStyle;
  }

  private CellStyle getDefaultCellStyle(Workbook workbook) {
    CellStyle cellStyle = workbook.getCellStyleAt(0);
    cellStyle.setLocked(false);
    return cellStyle;
  }

  private CellStyle createRowNumberStyle(Workbook workbook) {
    Font rowNumberFont = workbook.createFont();
    rowNumberFont.setBold(true);

    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setFont(rowNumberFont);
    cellStyle.setLocked(true);
    return cellStyle;
  }

  private CellStyle createInvalidCellStyle(Workbook workbook) {
    CellStyle invalidCellStyle = workbook.createCellStyle();
    invalidCellStyle.setFillBackgroundColor(new XSSFColor(getErrorBackgroundColor(), null));
    invalidCellStyle.setLocked(false);
    return invalidCellStyle;
  }

  private void onCellValueChanged(CellValueChangeEvent cellValueChangeEvent) {
    List<Cell> changedCells = cellValueChangeEvent.getChangedCells().stream()
        .map(this::getCell)
        .toList();
    updateModel(changedCells);
    if (validationMode == ValidationMode.EAGER) {
      updateValidation(changedCells);
    }
    autofitColumns(changedCells);
    delegateSpreadsheet.refreshCells(changedCells);
  }

  private Cell getCell(CellReference cellReference) {
    return getCell(cellReference.getRow(), cellReference.getCol());
  }

  private Cell getCell(int rowIndex, int colIndex) {
    return delegateSpreadsheet.getCell(rowIndex, colIndex);
  }

  private int rowIndex(Row row) {
    int rowIndex = rows.indexOf(row);
    if (rowIndex < 0) {
      throw new IllegalArgumentException("Row " + row + " is not contained.");
    }
    return rowIndex;
  }

  private Row getRow(int rowIndex) {
    return rows.get(rowIndex);
  }

  private int colIndex(Column<T> column) {
    int colIndex = columns.indexOf(column);
    if (colIndex < 0) {
      throw new IllegalArgumentException("Column " + column + " is not contained.");
    }
    return colIndex;
  }

  private Column<T> getColumn(int colIndex) {
    return columns.get(colIndex);
  }

  private void updateModel(List<Cell> changedCells) {
    for (Cell cell : changedCells) {
      Column<T> column = getColumn(cell.getColumnIndex());
      var row = getRow(cell.getRowIndex());
      BiConsumer<T, String> modelUpdater = column.modelUpdater;
      if (row instanceof DataRow dataRow) {
        modelUpdater.accept(dataRow.data(), getCellValue(cell));
      }
    }
  }

  private void autofitColumns(List<Cell> changedCells) {
    changedCells.stream().map(Cell::getColumnIndex)
        .distinct()
        .forEach(this::autoFitColumnWidth);
  }

  private void updateValidation(List<Cell> changedCells) {
    this.setInvalid(false);
    for (Cell changedCell : changedCells) {
      ValidationResult validationResult = validateCell(changedCell);
      if (validationResult.isInvalid()) {
        this.setInvalid(true);
      }
      if (hasCellValidationChanged(changedCell, validationResult)) {
        updateCellValidationStatus(changedCell, validationResult);
      }
    }
  }


  private void createCellsForRow(Row row) {
    List<Cell> cellsInRow = new ArrayList<>();
    for (int colIndex = 0; colIndex < columnCount(); colIndex++) {
      Cell cell = createCell(rowIndex(row), colIndex);
      cellsInRow.add(cell);
    }
    delegateSpreadsheet.refreshCells(cellsInRow);
  }

  private List<Cell> createCellsForColumn(Column<T> column) {
    int colIndex = colIndex(column);
    List<Cell> dirtyCells = new ArrayList<>();

    for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
      //FIXME in Java21 this if-else can be replace by switch expression
      Cell cell = createCell(rowIndex, colIndex);
      dirtyCells.add(cell);
    }
    return dirtyCells;
  }

  private Cell createCell(int rowIndex, int colIndex) {
    Column<T> column = getColumn(colIndex);
    Row row = getRow(rowIndex);
    //FIXME in Java 21 this can be replaced by a switch expression
    Cell cell;
    if (row instanceof HeaderRow) {
      cell = setCell(rowIndex, colIndex, column.getName(), columnHeaderStyle);
    } else if (row instanceof DataRow dataRow) {
      cell = setCell(rowIndex, colIndex, column.toCellValue.apply(dataRow.data()),
          column.getCellStyle().orElse(defaultCellStyle));
    } else {
      throw new IllegalStateException("Unexpected class of row: " + row);
    }
    return cell;
  }


  private void autoFitColumnWidth(int colIndex) {
    delegateSpreadsheet.autofitColumn(colIndex);
    int fittingColumnWidth = (int) delegateSpreadsheet.getActiveSheet().getColumnWidthInPixels(
        colIndex);
    int defaultColumnWidth = delegateSpreadsheet.getDefaultColumnWidth();
    delegateSpreadsheet.setColumnWidth(colIndex, Math.max(fittingColumnWidth, defaultColumnWidth));
  }


  private void deleteRow(int index) {
    int lastRowIndex = rowCount() - 1;
    int nextRowIndex = index + 1;
    if (index > lastRowIndex) {
      throw new IllegalArgumentException(
          "There is no row at index " + index + ". There are only rows with index up to "
              + lastRowIndex);
    }
    if (index < 0) {
      throw new IllegalArgumentException(
          "The row at index " + index
              + " cannot be removed. Please provide any index greater than 0");
    }
    Row row = getRow(index);
    if (row instanceof HeaderRow headerRow) {
      log.debug("Will not remove header row " + headerRow);
      return;
    }
    delegateSpreadsheet.deleteRows(index, index);
    if (nextRowIndex <= lastRowIndex) {
      delegateSpreadsheet.shiftRows(nextRowIndex, lastRowIndex, -1, true, true);
    }
    rows.remove(index);
    delegateSpreadsheet.setMaxRows(lastRowIndex);
  }


  private int dataRowCount() {
    return getData().size();
  }

  private int rowCount() {
    return rows.size();
  }

  private int columnCount() {
    return columns.size();
  }

  private List<Cell> cells() {
    List<Cell> cells = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
      for (int colIndex = 0; colIndex < columnCount(); colIndex++) {
        cells.add(getCell(rowIndex, colIndex));
      }
    }
    return cells.stream().toList();
  }

  private void markCellAsInvalid(Cell cell, String errorMessage) {
    if (rowNumberStyle.equals(cell.getCellStyle())) {
      return; // does not apply to row numbers
    }
    if (columnHeaderStyle.equals(cell.getCellStyle())) {
      return; // does not apply to column headers
    }
    Comment cellComment = createComment(errorMessage);
    cell.setCellComment(cellComment);
    cell.setCellStyle(invalidCellStyle);
  }

  private void markCellAsValid(Cell cell) {
    if (!invalidCellStyle.equals(cell.getCellStyle())) {
      return; // only apply to invalid cells
    }
    cell.setCellStyle(defaultCellStyle);
    cell.setCellComment(null);
  }

  private Comment createComment(String comment) {
    Comment cellComment = drawingPatriarch.createCellComment(creationHelper.createClientAnchor());
    cellComment.setString(new XSSFRichTextString(comment));
    return cellComment;
  }

  private void updateCellValidationStatus(Cell cell, ValidationResult validationResult) {
    if (validationResult.isValid()) {
      markCellAsValid(cell);
    } else {
      markCellAsInvalid(cell, validationResult.errorMessage());
    }
  }

  private boolean hasCellValidationChanged(Cell cell, ValidationResult validationResult) {
    if (isCellValid(cell) && validationResult.isValid()) {
      return false;
    }
    Comment existingComment = cell.getCellComment();
    if (isNull(existingComment)) {
      return true;
    }
    boolean validationMessageChanged = !validationResult.errorMessage()
        .equals(existingComment.getString().getString());
    return isCellValid(cell) || validationMessageChanged;
  }


  private boolean isCellValid(Cell cell) {
    return !isCellInvalid(cell);
  }

  private boolean isCellInvalid(Cell cell) {
    return invalidCellStyle.equals(cell.getCellStyle());
  }

  private static Color getErrorBackgroundColor() {
    float alpha = 0.1f;
    float hueAngle = 0f; // 0: red; 120: green, 240: blue
    float brightness = 1f; // blended with white
    return Color.getHSBColor(hueAngle, alpha, brightness);
  }

  private ValidationResult validateCell(Cell cell) {
    Column<T> column = getColumn(cell.getColumnIndex());
    List<ColumnValidator<String>> validators = column.getValidators();
    return validators.stream()
        .map(it -> it.validate(getCellValue(cell)))
        .filter(ValidationResult::isInvalid)
        .findAny()
        .orElse(ValidationResult.valid());
  }

  private String getCellValue(Cell cell) {
    return delegateSpreadsheet.getCellValue(cell);
  }

  private void setCell(Cell cell, String cellValue) {
    setCell(cell.getRowIndex(), cell.getColumnIndex(), cellValue, cell.getCellStyle());
  }

  private Cell setCell(int rowIndex, int colIndex, String cellValue, CellStyle cellStyle) {
    Cell cell = Optional.ofNullable(getCell(rowIndex, colIndex))
        .orElse(delegateSpreadsheet.createCell(rowIndex, colIndex, null));
    CellFunctions.setCellValue(cell, cellValue);
    cell.setCellStyle(cellStyle);

    //Please note: By default vaadin only fires CellValueChangeEvent when editing using the default inline editor
    // we fire an appropriate event here as we want to make sure it is thrown when a cell is updated using a custom editor as well
    onCellValueChanged(new CellValueChangeEvent(delegateSpreadsheet,
        Set.of(
            new CellReference(cell.getRowIndex(), cell.getColumnIndex()))));
    return cell;
  }

  private abstract class Row {
  }

  private final class DataRow extends Row {

    private final T data;

    private DataRow(T data) {
      requireNonNull(data, "data must not be null");
      this.data = data;
    }

    public T data() {
      return data;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      DataRow dataRow = (DataRow) object;

      return Objects.equals(data, dataRow.data);
    }

    @Override
    public int hashCode() {
      return data != null ? data.hashCode() : 0;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", DataRow.class.getSimpleName() + "[", "]")
          .add("data=" + data)
          .toString();
    }
  }

  private class HeaderRow extends Row {
  }

  public static class Column<T> {

    private final String name;
    private final List<ColumnValidator<String>> validators;

    private final Function<T, String> toCellValue;
    private final BiConsumer<T, String> modelUpdater;

    private Component editorComponent;
    private boolean required;
    private CellStyle cellStyle;

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

    List<ColumnValidator<String>> getValidators() {
      return Collections.unmodifiableList(validators);
    }

    public Optional<CellStyle> getCellStyle() {
      return Optional.ofNullable(cellStyle);
    }

    public Column<T> withValidator(Predicate<String> predicate, String errorMessage) {
      validators.add(new ColumnValidator<>(predicate, errorMessage));
      return this;
    }

    private static <E> ComponentRenderer<Component, E> getDefaultComponentRenderer() {
      return new ComponentRenderer<>(item -> {
        Span listItem = new Span(item.toString());
        listItem.addClassName("spreadsheet-list-item");
        return listItem;
      });
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
          "{0} is not a valid option for column %s. Please choose from %s".formatted(getName(),
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
      validators.addFirst(new ColumnValidator<>(
          object -> (Objects.nonNull(object) && !object.isBlank()) || !this.isRequired(),
          "The column '" + getName() + "' does not allow empty values. Please enter a value."));
      return this;
    }
  }

  public static class ColumnValidator<T2> {

    private final Predicate<T2> predicate;
    private final String errorMessage;

    ColumnValidator(Predicate<T2> predicate, String errorMessage) {
      this.predicate = predicate;
      this.errorMessage = errorMessage;
    }

    public ValidationResult validate(T2 value) {
      boolean isValid = predicate.test(value);
      String filledErrorMessage = errorMessage.replaceAll("\\{0\\}", String.valueOf(value));
      return isValid ? ValidationResult.valid() : ValidationResult.invalid(filledErrorMessage);
    }

    public record ValidationResult(boolean isValid, String errorMessage) {

      public ValidationResult {
        if (isValid) {
          errorMessage = "";
        }
      }

      public static ValidationResult valid() {
        return new ValidationResult(true, "");
      }

      public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
      }

      public boolean isInvalid() {
        return !isValid();
      }
    }
  }

  public static class SelectEditor<E> extends Select<E> {

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

  private class MyComponentFactory implements SpreadsheetComponentFactory {

    @Override
    public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex,
        com.vaadin.flow.component.spreadsheet.Spreadsheet spreadsheet, Sheet sheet) {
      return null; // we want the editor instead
    }

    @Override
    public Component getCustomEditorForCell(Cell cell, int rowIndex, int columnIndex,
        com.vaadin.flow.component.spreadsheet.Spreadsheet spreadsheet, Sheet sheet) {
      //FIXME why do indices start at -2 in the default vaadin implementation?
      if ((columnIndex < 0 || rowIndex < 0)
          || (columnIndex >= columnCount() || rowIndex >= rowCount())) {
        return null;
      }
      return getColumn(columnIndex).getEditorComponent().orElse(null);
    }

    @Override
    public void onCustomEditorDisplayed(Cell cell, int rowIndex, int columnIndex,
        com.vaadin.flow.component.spreadsheet.Spreadsheet spreadsheet, Sheet sheet,
        Component customEditor) {
      try {
        if (customEditor instanceof SelectEditor selectEditor) {

          selectEditor.removeAllValueChangeListeners();

          selectEditor.setFromCellValue(getCellValue(cell));

          selectEditor.addValueChangeListener(event -> {
            String cellValue = selectEditor.toCellValue(event.getValue());
            setCell(cell, cellValue);
            delegateSpreadsheet.refreshCells(cell);
          });
        }
      } catch (ClassCastException e) {
        log.debug("Seems not to be a SelectEditor.", e);
      }
    }
  }

  private static final class CellFunctions {

    static void setCellValue(Cell cell, String value) {
      switch (cell.getCellType()) {
        case _NONE, ERROR, FORMULA -> {
        }
        case NUMERIC -> cell.setCellValue(Double.parseDouble(value));
        case STRING, BLANK -> cell.setCellValue(value);
        case BOOLEAN -> cell.setCellValue(Boolean.parseBoolean(value));
        default -> throw new IllegalStateException("Unexpected value: " + cell.getCellType());
      }
    }
  }
}
