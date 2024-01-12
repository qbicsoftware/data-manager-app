package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.spreadsheet.Spreadsheet.CellValueChangeEvent;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import com.vaadin.flow.shared.Registration;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.general.spreadsheet.validation.ValidationResult;
import life.qbic.logging.api.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFColor;

/**
 * This spreadsheet component can be used to show data beans in configurable rows.
 * <p>
 * It offers several features:
 * <ul>
 *   <li> a validation mode EAGER (validation after cell change) or LAZY (validation only when calling {@link #validate()}
 *   <li> adding and removing rows
 *   <li> adding configurable columns
 * </ul>
 * The spreadsheet itself provides validation information, and an error message.
 */
@Tag(Tag.DIV)
public class Spreadsheet<T> extends Component implements HasComponents,
    HasValidationProperties {

  private static final Logger log = logger(Spreadsheet.class);

  private final com.vaadin.flow.component.spreadsheet.Spreadsheet delegateSpreadsheet = new com.vaadin.flow.component.spreadsheet.Spreadsheet();
  private final List<Column<T, ?>> columns = new ArrayList<>();
  private final List<Row> rows = new ArrayList<>();
  private List<Cell> currentInvalidCells = new ArrayList<>();

  // cell styles
  private final transient CellStyle defaultCellStyle;
  private final transient CellStyle lockedCellStyle;
  private final transient CellStyle invalidCellStyle;
  private final transient CellStyle rowNumberStyle;
  private final transient CellStyle columnHeaderStyle;

  protected ValidationMode validationMode;

  //ATTENTION: we need to hard-code this. We cannot ensure that the Calibri font is installed.
  // This value might need to change based on the font size or font family in the spreadsheet cells.
  private static final double CHARACTER_PIXEL_WIDTH = 9.0;

  public Spreadsheet() {
    addClassName("spreadsheet-container");
    delegateSpreadsheet.setActiveSheetProtected("");

    defaultCellStyle = getDefaultCellStyle(delegateSpreadsheet.getWorkbook());
    lockedCellStyle = createLockedCellStyle(delegateSpreadsheet.getWorkbook());
    invalidCellStyle = createInvalidCellStyle(delegateSpreadsheet.getWorkbook());
    rowNumberStyle = createRowNumberStyle(delegateSpreadsheet.getWorkbook());
    columnHeaderStyle = createColumnNameStyle(delegateSpreadsheet.getWorkbook());

    delegateSpreadsheet.setSheetSelectionBarVisible(false);
    delegateSpreadsheet.setFunctionBarVisible(false);
    delegateSpreadsheet.setRowColHeadingsVisible(false);
    delegateSpreadsheet.addCellValueChangeListener(this::onCellValueChanged);
    delegateSpreadsheet.setSizeFull();
    delegateSpreadsheet.setSpreadsheetComponentFactory(new SpreadsheetEditorComponentFactory());
    setErrorMessage("Please complete the missing mandatory information.");

    delegateSpreadsheet.setMaxRows(rowCount());
    add(delegateSpreadsheet);

    validationMode = ValidationMode.LAZY;
    Column<T, Integer> rowNumberColumn = addColumn("#",
        rowValue -> dataRowCount(),
        String::valueOf,
        (rowValue, cellValue) -> {/* do nothing */})
        .withCellStyle(rowNumberStyle);
    addHeaderRow();
  }

  /**
   * Adds a row to this spreadsheet displaying the provided data.
   *
   * @param rowData the data for this row
   */
  public void addRow(T rowData) {
    int previousRowCount = rowCount();
    var dataRow = new DataRow(rowData);
    rows.add(dataRow);
    createCellsForRow(dataRow);
    delegateSpreadsheet.setMaxRows(previousRowCount + 1);
  }

  /**
   * Clears the content of the spreadsheet. Removes all rows.
   */
  public void resetRows() {
    int lastRowIndex = rowCount() - 1;
    deleteRows(0, lastRowIndex);
    addHeaderRow();
    updateSpreadsheetValidity();
  }

  /**
   * Adds a column to the spreadsheet.
   *
   * @param name        the name of the column
   * @param toCellValue a function converting the row data to cell data in this column
   * @param modelEditor a bi-function that can be used to update the row data when cell data has
   *                    changed for this column
   * @return the added column
   */
  public Column<T, String> addColumn(String name, Function<T, String> toCellValue,
      BiConsumer<T, String> modelEditor) {
    return addColumn(name, toCellValue, identity(), modelEditor);
  }

  /**
   * Adds a column to the spreadsheet
   *
   * @param name                   the name of the column
   * @param toColumnValue          a function converting row data into data for this column
   * @param columnValueToCellValue a function converting column data to cell data
   * @param modelEditor            a bi-function that can be used to update the row data when cell
   *                               data has changed for this column
   * @param <C>                    the object type of this column
   * @return the created column
   */
  public <C> Column<T, C> addColumn(String name, Function<T, C> toColumnValue,
      Function<C, String> columnValueToCellValue,
      BiConsumer<T, String> modelEditor) {
    Column<T, C> column = new Column<>(name, toColumnValue, columnValueToCellValue, modelEditor);
    addColumn(column);
    return column;
  }

  private void addColumn(Column<T, ?> column) {
    columns.add(column);
    List<Cell> cellsForColumn = createCellsForColumn(column);
    refreshCells(cellsForColumn);
    delegateSpreadsheet.setMaxColumns(columnCount());
  }

  /**
   * Remove the last row from the spreadsheet, deleting contained information.
   */
  public void removeLastRow() {
    if (rowCount() == 0) {
      return;
    }
    int lastRowIndex = rowCount() - 1;
    Row lastRow = getRow(lastRowIndex);
    if (lastRow instanceof HeaderRow headerRow) {
      log.debug("Will not remove header row " + headerRow + " at " + lastRowIndex);
      return;
    }
    deleteRow(lastRowIndex);
    if (validationMode == ValidationMode.EAGER) {
      updateSpreadsheetValidity();
    }
  }

  /**
   * Changes the validation mode. If the validation mode is {@link ValidationMode#EAGER} a cell is
   * validated after it was updated. In {@link ValidationMode#LAZY} the validation is not triggered
   * after a cell is updated.
   *
   * @param validationMode the validation mode to use
   */
  public void setValidationMode(ValidationMode validationMode) {
    this.validationMode = validationMode;
  }

  public ValidationMode getValidationMode() {
    return validationMode;
  }

  /**
   * Get the data shown in the spreadsheet
   * @return the underlying data for every row as a List
   */
  public List<T> getData() {
    return rows.stream()
        .filter(row -> row instanceof DataRow)
        .map(row -> (DataRow) row)
        .map(DataRow::data)
        .toList();
  }

  /**
   *
   * @return true if the last validation passed; false otherwise
   */
  public boolean isValid() {
    return !isInvalid();
  }

  /**
   * Add a listener to the validation status of this spreadsheet.
   * @param listener the listener receiving validation changed events whenever the validation status changed.
   * @return a registration for the listener
   */
  public Registration addValidationChangeListener(
      ComponentEventListener<ValidationChangeEvent> listener) {
    return addListener(ValidationChangeEvent.class, listener);
  }

  /**
   * Returns the Cell belonging to a column with specified name and specified row index or empty
   * if that Cell is not found.
   * @param colName      the name of the column (as seen in the header)
   * @param dataRowIndex the index of the data row - as displayed in the left of the spreadsheet
   * @return a Cell object, or Optional.empty()
   */
  protected Optional<Cell> getCellByColNameAndRowIndex(String colName, int dataRowIndex) {
    String sampleRow = Integer.toString(dataRowIndex);
    int rowIndexCol = 0;
    for (Column<T, ?> col : columns) {
      if(col.getName().equals("#")) {
        break;
      }
      rowIndexCol++;
    }

    int colWithName = 0;
    for (Column<T, ?> col : columns) {
      if(col.getName().equals(colName)) {
        break;
      }
      colWithName++;
    }

    for(int rowIndex = 0; rowIndex < dataRowCount()+1; rowIndex++) {
      Optional<Cell> cell = getCell(rowIndex, rowIndexCol);
      if(sampleRow.equals(getCellValue(cell.orElseThrow()))) {
        return getCell(rowIndex, colWithName);
      }
    }

    return Optional.empty();
  }

  public static class ValidationChangeEvent extends ComponentEvent<Spreadsheet<?>> {

    private final boolean oldValue;
    private final boolean value;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ValidationChangeEvent(Spreadsheet<?> source, boolean fromClient, boolean wasValid,
        boolean isValid) {
      super(source, fromClient);
      this.oldValue = wasValid;
      this.value = isValid;
    }

    public boolean wasValid() {
      return oldValue;
    }

    public boolean isValid() {
      return value;
    }

    public boolean isInvalid() {
      return !isValid();
    }

  }

  private CellStyle createColumnNameStyle(Workbook workbook) {
    Font columnNameFont = workbook.createFont();
    columnNameFont.setBold(true);
    columnNameFont.setFontHeightInPoints((short) 11);
    columnNameFont.setFontName("Arial");

    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setFillBackgroundColor(null);
    cellStyle.setFont(columnNameFont);
    cellStyle.setAlignment(HorizontalAlignment.CENTER);

    cellStyle.setLocked(true);
    return cellStyle;
  }

  private static CellStyle getDefaultCellStyle(Workbook workbook) {
    Font defaultFont = workbook.createFont();
    defaultFont.setFontHeightInPoints((short) 11);
    defaultFont.setFontName("Arial");

    CellStyle cellStyle = workbook.getCellStyleAt(0);
    cellStyle.setFont(defaultFont);
    cellStyle.setLocked(false);
    return cellStyle;
  }

  private static CellStyle createLockedCellStyle(Workbook workbook) {
    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setLocked(true);
    return cellStyle;
  }

  private static CellStyle createRowNumberStyle(Workbook workbook) {
    Font rowNumberFont = workbook.createFont();
    rowNumberFont.setBold(true);
    rowNumberFont.setFontHeightInPoints((short) 11);
    rowNumberFont.setFontName("Arial");

    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setFont(rowNumberFont);
    cellStyle.setAlignment(HorizontalAlignment.CENTER);
    cellStyle.setLocked(true);
    return cellStyle;
  }

  private CellStyle createInvalidCellStyle(Workbook workbook) {
    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setFillBackgroundColor(new XSSFColor(getErrorBackgroundColor(), null));
    cellStyle.setLocked(false);
    return cellStyle;
  }

  private void onCellValueChanged(CellValueChangeEvent cellValueChangeEvent) {
    List<Cell> changedCells = cellValueChangeEvent.getChangedCells().stream()
        .map(this::getCell)
        .filter(Optional::isPresent).map(Optional::get)
        .toList();
    refreshCellData(changedCells);
    refreshCells(changedCells);
  }

  //<editor-fold desc="Content manipulation">

  private void addHeaderRow() {
    int previousRowCount = rowCount();
    var headerRow = new HeaderRow();
    rows.add(headerRow);
    createCellsForRow(headerRow);
    delegateSpreadsheet.setMaxRows(previousRowCount + 1);
  }

  /**
   * Refreshes the background information on a cell. Does not redraw the cell.
   * <p>
   * Background data in this case are
   * <ul>
   *   <li/> model data, e.g. the bean values
   *   <li/> validation status if eagerly evaluated
   *   <li/> column width
   *
   * @param cells the cells for which to refresh the background data.
   */
  private void refreshCellData(List<Cell> cells) {
    updateModel(cells);
    if (validationMode == ValidationMode.EAGER) {
      updateValidation(cells);
    }
    autofitColumns(cells);
  }

  /**
   * Updates the underlying data structure in case the cell is in a {@link DataRow}.
   *
   * @param changedCells the cells for which to trigger the data update.
   */
  private void updateModel(List<Cell> changedCells) {
    for (Cell cell : changedCells) {
      Column<T, ?> column = getColumn(cell.getColumnIndex());
      var row = getRow(cell.getRowIndex());
      BiConsumer<T, String> modelUpdater = column.modelUpdater();
      if (row instanceof DataRow dataRow) {
        modelUpdater.accept(dataRow.data(), getCellValue(cell));
      }
    }
  }

  /**
   * Fits the columns for the cells to the content
   *
   * @param changedCells the cells for which to fit the column width
   */
  private void autofitColumns(List<Cell> changedCells) {
    changedCells.stream().map(Cell::getColumnIndex)
        .distinct()
        .forEach(this::autoFitColumnWidth);
  }

  /**
   * Runs validation on the provided cells. If all cells are valid, sets the spreadsheet to be valid
   * as well; otherwise sets the spreadsheet to be invalid.
   *
   * @param changedCells the cells to validate
   */
  private void updateValidation(List<Cell> changedCells) {
    for (Cell changedCell : changedCells) {
      ValidationResult validationResult = validateCell(changedCell);
      if (hasCellValidationChanged(changedCell, validationResult)) {
        updateCellValidationStatus(changedCell, validationResult);
      }
    }
  }

  private void updateSpreadsheetValidity() {
    List<Cell> newInvalidCells = cells().stream().filter(this::isCellInvalid).toList();
    boolean wasInvalid = !currentInvalidCells.isEmpty();
    boolean willBeInvalid = !newInvalidCells.isEmpty();

    // we compare if the list of invalid cells changes, as this might lead to change of error message
    boolean invalidationReasonChanged = !currentInvalidCells.equals(newInvalidCells);

    setInvalid(willBeInvalid);
    currentInvalidCells = newInvalidCells;

    if (wasInvalid != willBeInvalid || invalidationReasonChanged) {
      fireEvent(new ValidationChangeEvent(this, false, !wasInvalid, !willBeInvalid));
    }
  }

  private void createCellsForRow(Row row) {
    List<Cell> cellsInRow = new ArrayList<>();
    for (int colIndex = 0; colIndex < columnCount(); colIndex++) {
      Cell cell = createCell(rowIndex(row), colIndex);
      cellsInRow.add(cell);
    }
    refreshCells(cellsInRow);
  }

  protected void refreshCells(Collection<Cell> cells) {
    updateSpreadsheetValidity();
    delegateSpreadsheet.refreshCells(cells);
  }

  private List<Cell> createCellsForColumn(Column<T, ?> column) {
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
    Column<T, ?> column = getColumn(colIndex);
    Row row = getRow(rowIndex);
    //FIXME in Java 21 this can be replaced by a switch expression
    Cell cell;
    if (row instanceof HeaderRow) {
      cell = setCell(rowIndex, colIndex, column.getName(), columnHeaderStyle);
    } else if (row instanceof DataRow dataRow) {
      cell = setCell(rowIndex, colIndex, column.toCellValue(dataRow.data()),
          column.getCellStyle().orElse(defaultCellStyle));
    } else {
      throw new IllegalStateException("Unexpected class of row: " + row);
    }
    refreshCellData(List.of(cell));
    return cell;
  }

  private void updateCell(Cell cell, String value) {
    updateCell(cell.getRowIndex(), cell.getColumnIndex(), value, cell.getCellStyle());
  }

  private Cell updateCell(int rowIndex, int colIndex, String cellValue, CellStyle cellStyle) {
    Cell cell = setCell(rowIndex, colIndex, cellValue, cellStyle);
    //Please note: By default vaadin only fires CellValueChangeEvent when editing using the default inline editor
    // thus we need to run all corresponding actions here as well.
    refreshCellData(List.of(cell));
    return cell;
  }

  private Cell setCell(int rowIndex, int colIndex, String cellValue, CellStyle cellStyle) {
    Cell cell = getCell(rowIndex, colIndex)
        .orElse(delegateSpreadsheet.createCell(rowIndex, colIndex, null));
    setCellValue(cell, cellValue);
    cell.setCellStyle(cellStyle);
    return cell;
  }

  private static void setCellValue(Cell cell, String cellValue) {
    switch (cell.getCellType()) {
      case _NONE, ERROR, FORMULA -> {
        /* do nothing */
      }
      case NUMERIC -> cell.setCellValue(Double.parseDouble(cellValue));
      case STRING, BLANK -> cell.setCellValue(cellValue);
      case BOOLEAN -> cell.setCellValue(Boolean.parseBoolean(cellValue));
      default -> throw new IllegalStateException("Unexpected value: " + cell.getCellType());
    }
  }

  /**
   * Deletes a row at the given index if the row is a {@link DataRow}. Does nothing if the row is a
   * {@link HeaderRow}. Shifts following rows up if any exist.
   *
   * @param index the index of the row to remove
   */
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
              + " cannot be removed. Please provide any index greater or equal to 0");
    }
    deleteRows(index, index);
  }

  /**
   * Deletes all rows starting with startIndex inclusive up to endIndex inclusive.
   *
   * @param startIndex the index where to start from
   * @param endIndex   the index where to stop (inclusive)
   */
  private void deleteRows(int startIndex, int endIndex) {
    int numberOfRowsBeforeRemoval = rowCount();
    int lastRowIndex = numberOfRowsBeforeRemoval - 1;
    int nextRowIndex = endIndex + 1;
    if (startIndex > endIndex) {
      throw new IllegalArgumentException(
          "The start index " + startIndex + " must be greater or equal to the end index "
              + endIndex);
    }
    if (endIndex > lastRowIndex) {
      throw new IllegalArgumentException(
          "There is no row at index " + endIndex + ". There are only rows with index up to "
              + lastRowIndex);
    }
    if (startIndex < 0) {
      throw new IllegalArgumentException(
          "The row at index " + startIndex
              + " cannot be removed. Please provide any index greater than 0");
    }
    //FIXME currently needed due to https://github.com/vaadin/spreadsheet/issues/842
    // we need to select a cell and de-select it again after deletion
    CellReference selectedCellReference = delegateSpreadsheet.getSelectedCellReference();
    if (isNull(selectedCellReference)) {
      delegateSpreadsheet.setSelection(0, 0);
    }

    delegateSpreadsheet.deleteRows(startIndex, endIndex);

    if (isNull(selectedCellReference)) {
      delegateSpreadsheet.getCellSelectionManager().clear();
    }

    int numberOfRemovedRows = endIndex + 1 - startIndex;
    if (nextRowIndex <= lastRowIndex) {
      delegateSpreadsheet.shiftRows(nextRowIndex, lastRowIndex, -numberOfRemovedRows, true, true);
    }
    int numberOfRowsAfterRemoval = numberOfRowsBeforeRemoval - numberOfRemovedRows;
    for (int index = 0; index <= endIndex - startIndex; index++) {
      /*
      We cannot use removeAll as the equals method fits all empty rows equally.
      Thus, we need to delete using indices.
      When using indices to remove from a list all content to the right is shifted left.
      We need to remove from the same position again and again until we deleted the number of elements we wanted to delete.
       */
      rows.remove(startIndex);
    }
    delegateSpreadsheet.setMaxRows(numberOfRowsAfterRemoval);
  }

  /**
   * @return count the rows containing data.
   */
  private int dataRowCount() {
    return getData().size();
  }

  /**
   * @return the total number of rows including header rows.
   */
  private int rowCount() {
    return rows.size();
  }

  /**
   * @return the total number of columns in the spreadsheet
   */
  private int columnCount() {
    return columns.size();
  }


  private String getCellValue(Cell cell) {
    return delegateSpreadsheet.getCellValue(cell);
  }

  private Optional<Cell> getCell(CellReference cellReference) {
    return getCell(cellReference.getRow(), cellReference.getCol());
  }

  private Optional<Cell> getCell(int rowIndex, int colIndex) {
    if (rowIndex >= rowCount()) {
      return Optional.empty();
    }
    if (colIndex >= columnCount()) {
      return Optional.empty();
    }
    return Optional.ofNullable(delegateSpreadsheet.getCell(rowIndex, colIndex));
  }

  /**
   * @return all cells in the spreadsheet
   */
  private List<Cell> cells() {
    List<Cell> cells = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
      for (int colIndex = 0; colIndex < columnCount(); colIndex++) {
        final int finalRowIndex = rowIndex;
        final int finalColIndex = colIndex;
        getCell(rowIndex, colIndex).ifPresentOrElse(
            cells::add,
            () -> {
              throw new ApplicationException(
                  "Expected cell but found none at (row: " + finalRowIndex + "; column: "
                      + finalColIndex + ")");
            }
        );
      }
    }
    return cells.stream().toList();
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

  private int colIndex(Column<T, ?> column) {
    int colIndex = columns.indexOf(column);
    if (colIndex < 0) {
      throw new IllegalArgumentException("Column " + column + " is not contained.");
    }
    return colIndex;
  }

  private Column<T, ?> getColumn(int colIndex) {
    return columns.get(colIndex);
  }

  public void lockColumn(Column<T, ?> column) {
    if (!columns.contains(column)) {
      throw new IllegalArgumentException(
          "Cannot lock column. The column is not part of this spreadsheet");
    }
    column.withCellStyle(lockedCellStyle);
  }


  private void autoFitColumnWidth(int colIndex) {
    int defaultColumnWidth = delegateSpreadsheet.getDefaultColumnWidth();
    int longestCellValue = getColumnValues(colIndex).stream().mapToInt(String::length).max()
        .orElse(0);
    int requiredPixelWidth = (int) Math.ceil(CHARACTER_PIXEL_WIDTH * longestCellValue);
    delegateSpreadsheet.setColumnWidth(colIndex, Math.max(requiredPixelWidth, defaultColumnWidth));

  }

  private static Color getErrorBackgroundColor() {
    float alpha = 0.1f;
    float hueAngle = 0f; // 0: red; 120: green, 240: blue
    float brightness = 1f; // blended with white
    return Color.getHSBColor(hueAngle, alpha, brightness);
  }


  /**
   * Validate the spreadsheet. This method performs validation and updates the current validation status.
   *
   */
  public void validate() {
    List<Cell> cells = cells();
    updateValidation(cells);
    refreshCells(cells);
  }

  private List<String> getColumnValues(int columnIndex) {
    return IntStream.range(0, rowCount())
        .mapToObj(rowIndex -> getCell(rowIndex, columnIndex))
        .filter(Optional::isPresent).map(Optional::get)
        .map(this::getCellValue)
        .collect(Collectors.toList());
  }

  private ValidationResult validateCell(Cell cell) {
    Column<T, ?> column = getColumn(cell.getColumnIndex());
    Row row = getRow(cell.getRowIndex());
    if (row instanceof HeaderRow) {
      return ValidationResult.valid();
    }
    List<ValidationResult> cellValidationResults = column.getValidators()
        .stream()
        .map(it -> it.validate(getCellValue(cell)))
        .toList();

    T data = ((DataRow) row).data();

    List<ValidationResult> objectValidationResults = column.getObjectValidators()
        .stream()
        .map(it -> it.validate(data, getCellValue(cell)))
        .toList();

    List<String> columnValues = getColumnValues(cell.getColumnIndex());
    List<ValidationResult> columnValidationResults = column.getColumnValidators().stream()
        .map(it -> it.validate(columnValues, getCellValue(cell)))
        .toList();

    return Stream.of(cellValidationResults,
            objectValidationResults,
            columnValidationResults)
        .flatMap(List::stream)
        .filter(ValidationResult::isInvalid)
        .findAny()
        .orElse(ValidationResult.valid());
  }

  protected void markCellAsInvalid(Cell cell) {
    if (rowNumberStyle.equals(cell.getCellStyle())) {
      return; // does not apply to row numbers
    }
    if (columnHeaderStyle.equals(cell.getCellStyle())) {
      return; // does not apply to column headers
    }
    cell.setCellStyle(invalidCellStyle);
  }

  private void markCellAsValid(Cell cell) {
    if (!invalidCellStyle.equals(cell.getCellStyle())) {
      return; // only apply to invalid cells
    }
    cell.setCellStyle(defaultCellStyle);
  }

  private void updateCellValidationStatus(Cell cell, ValidationResult validationResult) {
    if (validationResult.isValid()) {
      markCellAsValid(cell);
    } else {
      markCellAsInvalid(cell);
    }
  }

  private boolean hasCellValidationChanged(Cell cell, ValidationResult validationResult) {
    return !(isCellValid(cell) && validationResult.isValid())
        || !(isCellInvalid(cell) && validationResult.isInvalid());
  }

  private boolean isCellValid(Cell cell) {
    return !isCellInvalid(cell);
  }

  private boolean isCellInvalid(Cell cell) {
    return invalidCellStyle.equals(cell.getCellStyle());
  }

  public enum ValidationMode {
    LAZY,
    EAGER
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

  /**
   * This SpreadsheetComponentFactory handles components in the spreadsheet. When a custom editor is
   * retrieved, the editor is taken from the corresponding column.
   */
  private final class SpreadsheetEditorComponentFactory implements SpreadsheetComponentFactory {

    @Override
    public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex,
        com.vaadin.flow.component.spreadsheet.Spreadsheet spreadsheet, Sheet sheet) {
      return null; // we want the editor instead
    }

    @Override
    public Component getCustomEditorForCell(Cell cell, int rowIndex, int columnIndex,
        com.vaadin.flow.component.spreadsheet.Spreadsheet spreadsheet, Sheet sheet) {
      //We need this as indices start at -2 in the default vaadin implementation.
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

          Row row = getRow(cell.getRowIndex());
          if (row instanceof DataRow dataRow) {
            selectEditor.updateItems(dataRow.data());
          }

          selectEditor.setFromCellValue(getCellValue(cell));

          selectEditor.addValueChangeListener(event -> {
            String cellValue = selectEditor.toCellValue(event.getValue());
            updateCell(cell, cellValue);
            updateSpreadsheetValidity();
            spreadsheet.refreshCells(cell);
          });
        }
      } catch (ClassCastException e) {
        log.debug("Cannot open select editor on cell [r:%s, c:%s]".formatted(cell.getRowIndex(),
            cell.getColumnIndex()), e);
      }
    }
  }
}
