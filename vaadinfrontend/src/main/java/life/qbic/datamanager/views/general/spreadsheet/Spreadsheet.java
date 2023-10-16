package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.ColumnValidator.ValidationResult;
import life.qbic.logging.api.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
  private final List<T> rows = new ArrayList<>();

  // cell styles
  private final CellStyle defaultCellStyle;
  private final CellStyle invalidCellStyle;
  private final CellStyle rowNumberStyle;
  private final CellStyle columnHeaderStyle;

  // apache helpers
  private final CreationHelper creationHelper;
  private final Drawing<?> drawingPatriarch;

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

    Column<T> rowNumberColumn = addColumn("",
        rowValue -> String.valueOf(rowCount()),
        (rowValue, cellValue) -> {/* do nothing */})
        .withCellStyle(rowNumberStyle);

    delegateSpreadsheet.setMaxRows(rowCount());
    add(delegateSpreadsheet);
  }

  private CellStyle createColumnNameStyle(Workbook workbook) {
    Font columnNameFont = workbook.createFont();
    columnNameFont.setBold(true);

    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setFillBackgroundColor(null);
    cellStyle.setFont(columnNameFont);

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
    invalidCellStyle.setFillBackgroundColor(toSpreadsheetColor(getErrorBackgroundColor()));
    invalidCellStyle.setLocked(false);
    return invalidCellStyle;
  }

  private void onCellValueChanged(CellValueChangeEvent cellValueChangeEvent) {
    List<Cell> changedCells = cellValueChangeEvent.getChangedCells().stream()
        .map(changedCell -> {
          int rowIndex = changedCell.getRow();
          int colIndex = changedCell.getCol();
          return delegateSpreadsheet.getCell(rowIndex, colIndex);
        }).toList();
    changedCells.forEach(
        cell -> columns.get(cell.getColumnIndex()).modelEditor.accept(rows.get(cell.getRowIndex()),
            delegateSpreadsheet.getCellValue(cell)));
    delegateSpreadsheet.refreshCells(changedCells);
  }

  public void addRow(T rowData) {
    createCellsForRow(rowData);
    rows.add(rowData);
    delegateSpreadsheet.setMaxRows(rowCount());
  }

  private void createCellsForRow(T rowData) {
    int rowIndex = rowCount();
    List<Cell> cellsInRow = new ArrayList<>();
    for (Column<T> column : columns) {
      int colIndex = columns.indexOf(column);

      String cellValue = column.toCellValue.apply(rowData);
      Cell cell = setCell(rowIndex, colIndex, cellValue,
          column.getCellStyle().orElse(defaultCellStyle));
      cellsInRow.add(cell);
    }
    delegateSpreadsheet.refreshCells(cellsInRow);
  }

  private List<Cell> createCellsForColumn(Column<T> column) {
    int colIndex = columns.indexOf(column);
    List<Cell> dirtyCells = new ArrayList<>();

    for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
      String cellValue = column.toCellValue.apply(rows.get(rowIndex));
      Cell cell = setCell(rowIndex, colIndex, cellValue,
          column.getCellStyle().orElse(defaultCellStyle));
      dirtyCells.add(cell);
    }
    return dirtyCells;
  }

  private Cell setCell(int rowIndex, int colIndex, String cellValue, CellStyle cellStyle) {
    Cell cell = Optional.ofNullable(delegateSpreadsheet.getCell(rowIndex, colIndex))
        .orElse(delegateSpreadsheet.createCell(rowIndex, colIndex, null));
    CellFunctions.setCellValue(cell, cellValue);
    cell.setCellStyle(cellStyle);
    delegateSpreadsheet.autofitColumn(colIndex);
    return cell;
  }



  public void removeLastRow() {
    if (rowCount() == 0) {
      return;
    }
    deleteRow(rowCount() - 1);
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

    delegateSpreadsheet.deleteRows(index, index);
    if (nextRowIndex <= lastRowIndex) {
      delegateSpreadsheet.shiftRows(nextRowIndex, lastRowIndex, -1, true, true);
    }
    rows.remove(index);
    delegateSpreadsheet.setMaxRows(lastRowIndex);
  }

  public Column<T> addColumn(String name, Function<T, String> toCellValue,
      BiConsumer<T, String> modelEditor) {
    Column<T> column = new Column<>(name, toCellValue, modelEditor);
    columns.add(column);
    delegateSpreadsheet.refreshCells(createCellsForColumn(column));
    delegateSpreadsheet.setMaxColumns(columnCount());
    return column;
  }

  private int rowCount() {
    return rows.size();
  }
  private int columnCount() {
    return columns.size();
  }

  public List<T> getRows() {
    return rows;
  }

  public boolean isValid() {
    return !isInvalid();
  }

  record CellReference(int rowIndex, int colIndex) {

  }

  private List<Cell> cells() {
    List<CellReference> cellReferences = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
      for (int colIndex = 0; colIndex < columnCount(); colIndex++) {
        cellReferences.add(new CellReference(rowIndex, colIndex));
      }
    }
    return cellReferences.stream()
        .map(cellReference -> delegateSpreadsheet.getCell(cellReference.rowIndex(),
            cellReference.colIndex()))
        .toList();
  }

  private boolean markCellAsInvalid(Cell cell, String errorMessage) {
    if (invalidCellStyle.equals(cell.getCellStyle())) {
      return false; // already invalid
    }
    if (rowNumberStyle.equals(cell.getCellStyle())) {
      return false; // does not apply to row numbers
    }
    if (columnHeaderStyle.equals(cell.getCellStyle())) {
      return false; // does not apply to column headers
    }

    cell.setCellStyle(invalidCellStyle);
    Comment cellComment = createComment(errorMessage);
    cell.setCellComment(cellComment);
    return true;
  }

  private boolean markCellAsValid(Cell cell) {
    if (!invalidCellStyle.equals(cell.getCellStyle())) {
      return false; // only apply to invalid cells
    }
    cell.setCellStyle(defaultCellStyle);
    cell.setCellComment(null);
    return true;
  }

  private Comment createComment(String comment) {
    Comment cellComment = drawingPatriarch.createCellComment(creationHelper.createClientAnchor());
    cellComment.setString(new XSSFRichTextString(comment));
    return cellComment;
  }

  public void validate() {

    List<Cell> updatedCells = new ArrayList<>();
    ValidationResult result = ValidationResult.valid();
    for (Cell cell : cells()) {
      ValidationResult validationResult = validateCell(cell);
      boolean wasCellUpdated;
      if (validationResult.isValid()) {
        wasCellUpdated = markCellAsValid(cell);
      } else {
        wasCellUpdated = markCellAsInvalid(cell, validationResult.errorMessage());
        result = validationResult;
      }
      if (wasCellUpdated) {
        updatedCells.add(cell);
      }
    }
    this.setInvalid(result.isInvalid());
    delegateSpreadsheet.refreshCells(updatedCells);
  }

  private static Color getErrorBackgroundColor() {
    float alpha = 0.1f;
    float hueAngle = 0f; // 0: red; 120: green, 240: blue
    float brightness = 1f; // blended with white
    return Color.getHSBColor(hueAngle, alpha, brightness);
  }

  private static org.apache.poi.ss.usermodel.Color toSpreadsheetColor(Color color) {
    return new XSSFColor(color, null);
  }


  private ValidationResult validateCell(Cell cell) {
    Column<T> column = columns.get(cell.getColumnIndex());
    List<ColumnValidator<String>> validators = column.getValidators();
    Stream<ValidationResult> validationResultStream = validators.stream()
        .map(it -> {
          return it.validate(delegateSpreadsheet.getCellValue(cell));
        });
    ValidationResult validationResult = validationResultStream.filter(
            ValidationResult::isInvalid).findAny()
        .orElse(ValidationResult.valid());
    fireEvent(new CellValidationEvent(this, false, cell.getRowIndex(), cell.getColumnIndex(),
        validationResult));
    return validationResult;
  }

  private void updateCell(Cell cell, String value) {
    CellFunctions.setCellValue(cell, value);
    //Please note: By default vaadin only fires CellValueChangeEvent when editing using the default inline editor
    // we fire an appropriate event here as we want to make sure it is thrown when a cell is updated
    // Therefore when implementing a custom editor, call this method!
    onCellValueChanged(new CellValueChangeEvent(delegateSpreadsheet,
        Set.of(
            new org.apache.poi.ss.util.CellReference(cell.getRowIndex(), cell.getColumnIndex()))));
  }


  private static class CellFunctions {

    static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    static void setCellValue(Cell cell, String value) {
      switch (cell.getCellType()) {
        case _NONE, ERROR, FORMULA -> {
        }
        case NUMERIC -> {
          try {
            cell.setCellValue(numberFormat.parse(value).doubleValue());
          } catch (ParseException e) {
            throw new RuntimeException(e);
          }
        }
        case STRING, BLANK -> cell.setCellValue(value);
        case BOOLEAN -> cell.setCellValue(Boolean.parseBoolean(value));
        default -> throw new IllegalStateException("Unexpected value: " + cell.getCellType());
      }
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
      //FIXME why do indices start at -2?
      if ((columnIndex < 0 || rowIndex < 0)
          || (columnIndex >= columnCount() || rowIndex >= rowCount())) {
        return null;
      }
      return columns.get(columnIndex).getEditorComponent().orElse(null);
    }

    @Override
    public void onCustomEditorDisplayed(Cell cell, int rowIndex, int columnIndex,
        com.vaadin.flow.component.spreadsheet.Spreadsheet spreadsheet, Sheet sheet,
        Component customEditor) {
      try {
        if (customEditor instanceof SelectEditor selectEditor) {

          selectEditor.removeAllValueChangeListeners();

          selectEditor.setFromCellValue(delegateSpreadsheet.getCellValue(cell));

          selectEditor.addValueChangeListener(event -> {
            String cellValue = selectEditor.toCellValue(event.getValue());
            updateCell(cell, cellValue);
            delegateSpreadsheet.refreshCells(cell);
          });
        }
      } catch (ClassCastException e) {
        log.debug("Seems not to be a SelectEditor.", e);
      }
    }
  }


  public static class Column<T> {

    private final String name;
    private final List<ColumnValidator<String>> validators;

    private final Function<T, String> toCellValue;
    private final BiConsumer<T, String> modelEditor;

    private Component editorComponent;
    private boolean required;
    private CellStyle cellStyle;

    public Column(String name, Function<T, String> toCellValue, BiConsumer<T, String> modelEditor) {
      requireNonNull(name, "name must not be null");
      requireNonNull(toCellValue, "toCellValue must not be null");
      requireNonNull(modelEditor, "modelEditor must not be null");
      this.name = name;
      this.toCellValue = toCellValue;
      this.modelEditor = modelEditor;
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
      validators.add(0, new ColumnValidator<>(
          object -> (Objects.nonNull(object) && !object.isBlank()) || !this.isRequired(),
          "The column " + getName() + " does not allow empty values. Please enter a value."));
      return this;
    }

  }

  public static class CellValidationEvent extends ComponentEvent<Spreadsheet<?>> {

    private final int rowIndex;
    private final int colIndex;
    private final ValidationResult validationResult;


    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CellValidationEvent(Spreadsheet<?> source, boolean fromClient, int rowIndex,
        int colIndex, ValidationResult validationResult) {
      super(source, fromClient);
      this.rowIndex = rowIndex;
      this.colIndex = colIndex;
      this.validationResult = validationResult;
    }

    public int rowIndex() {
      return rowIndex;
    }

    public int colIndex() {
      return colIndex;
    }

    public ValidationResult validationResult() {
      return validationResult;
    }
  }
}
