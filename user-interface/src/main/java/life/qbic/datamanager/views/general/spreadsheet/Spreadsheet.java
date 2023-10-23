package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.requireNonNull;
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
import life.qbic.datamanager.views.general.spreadsheet.ColumnValidator.ValidationResult;
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

  public void addRow(T rowData) {
    int previousRowCount = rowCount();
    var dataRow = new DataRow(rowData);
    rows.add(dataRow);
    createCellsForRow(dataRow);
    delegateSpreadsheet.setMaxRows(previousRowCount + 1);
  }

  public Column<T> addColumn(String name, Function<T, String> toCellValue,
      BiConsumer<T, String> modelEditor) {
    Column<T> column = new Column<>(name, toCellValue, modelEditor);
    columns.add(column);
    List<Cell> cellsForColumn = createCellsForColumn(column);
    refreshCells(cellsForColumn);
    delegateSpreadsheet.setMaxColumns(columnCount());
    return column;
  }

  public void removeLastRow() {
    if (rowCount() == 0) {
      return;
    }
    deleteRow(rowCount() - 1);
    if (validationMode == ValidationMode.EAGER) {
      updateSpreadsheetValidity();
    }
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

  public Registration addValidationChangeListener(
      ComponentEventListener<ValidationChangeEvent> listener) {
    return addListener(ValidationChangeEvent.class, listener);
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
    public ValidationChangeEvent(Spreadsheet source, boolean fromClient, boolean oldValue,
        boolean value) {
      super(source, fromClient);
      this.oldValue = oldValue;
      this.value = value;
    }

    public boolean oldValue() {
      return oldValue;
    }

    public boolean value() {
      return value;
    }
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
      Column<T> column = getColumn(cell.getColumnIndex());
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
    boolean wasInvalid = isInvalid();
    boolean willBeInvalid = cells().stream().anyMatch(this::isCellInvalid);
    setInvalid(willBeInvalid);
    if (wasInvalid != willBeInvalid) {
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

  private void refreshCells(Collection<Cell> cells) {
    updateSpreadsheetValidity();
    delegateSpreadsheet.refreshCells(cells);
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
    Cell cell = Optional.ofNullable(getCell(rowIndex, colIndex))
        .orElse(delegateSpreadsheet.createCell(rowIndex, colIndex, null));
    setCellValue(cell, cellValue);
    cell.setCellStyle(cellStyle);
    return cell;
  }

  private static void setCellValue(Cell cell, String cellValue) {
    switch (cell.getCellType()) {
      case _NONE, ERROR, FORMULA -> {
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

  private Cell getCell(CellReference cellReference) {
    return getCell(cellReference.getRow(), cellReference.getCol());
  }

  private Cell getCell(int rowIndex, int colIndex) {
    return delegateSpreadsheet.getCell(rowIndex, colIndex);
  }

  /**
   * @return all cells in the spreadsheet
   */
  private List<Cell> cells() {
    List<Cell> cells = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
      for (int colIndex = 0; colIndex < columnCount(); colIndex++) {
        cells.add(getCell(rowIndex, colIndex));
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


  private void autoFitColumnWidth(int colIndex) {
    delegateSpreadsheet.autofitColumn(colIndex);
    int fittingColumnWidth = (int) delegateSpreadsheet.getActiveSheet().getColumnWidthInPixels(
        colIndex);
    int defaultColumnWidth = delegateSpreadsheet.getDefaultColumnWidth();
    delegateSpreadsheet.setColumnWidth(colIndex, Math.max(fittingColumnWidth, defaultColumnWidth));
  }

  private Comment createComment(String comment) {
    Comment cellComment = drawingPatriarch.createCellComment(creationHelper.createClientAnchor());
    cellComment.setString(new XSSFRichTextString(comment));
    return cellComment;
  }

  private static Color getErrorBackgroundColor() {
    float alpha = 0.1f;
    float hueAngle = 0f; // 0: red; 120: green, 240: blue
    float brightness = 1f; // blended with white
    return Color.getHSBColor(hueAngle, alpha, brightness);
  }


  public void validate() {
    List<Cell> cells = cells();
    updateValidation(cells);
    refreshCells(cells);
  }

  private ValidationResult validateCell(Cell cell) {
    Column<T> column = getColumn(cell.getColumnIndex());
    Row row = getRow(cell.getRowIndex());
    if (row instanceof HeaderRow) {
      return ValidationResult.valid();
    }
    List<ColumnValidator<String>> validators = column.getValidators();
    return validators.stream()
        .map(it -> it.validate(getCellValue(cell)))
        .filter(ValidationResult::isInvalid)
        .findAny()
        .orElse(ValidationResult.valid());
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
    cell.removeCellComment();
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
    if (isCellInvalid(cell) && validationResult.isInvalid()) {
      Comment existingComment = cell.getCellComment();
      assert Objects.nonNull(validationResult.errorMessage()) : "error message is never null";
      if (Objects.isNull(existingComment)) {
        // error message is never null
        return true;
      }
      return !validationResult.errorMessage()
          .equals(existingComment.getString().getString());
    }
    return true;
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
  private final class MyComponentFactory implements SpreadsheetComponentFactory {

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
          selectEditor.setFromCellValue(getCellValue(cell));
          selectEditor.addValueChangeListener(event -> {
            String cellValue = selectEditor.toCellValue(event.getValue());
            updateCell(cell, cellValue);
            updateSpreadsheetValidity();
            spreadsheet.refreshCells(cell);
          });
        }
      } catch (ClassCastException e) {
        log.debug("Seems not to be a SelectEditor.", e);
      }
    }
  }
}
