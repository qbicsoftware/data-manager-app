package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.spreadsheet.Spreadsheet.CellValueChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.poi.ss.usermodel.Cell;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class Spreadsheet<T> extends Div {

  private boolean valid = true;

  private final com.vaadin.flow.component.spreadsheet.Spreadsheet delegateSpreadsheet = new com.vaadin.flow.component.spreadsheet.Spreadsheet();
  private final List<Column<T, String>> columns = new ArrayList<>();
  private final List<T> rows = new ArrayList<>();

  public Spreadsheet() {
    addClassName("spreadsheet-container");
    delegateSpreadsheet.setSheetSelectionBarVisible(false);
    delegateSpreadsheet.setFunctionBarVisible(false);
    delegateSpreadsheet.setRowColHeadingsVisible(false);
    delegateSpreadsheet.addCellValueChangeListener(this::onCellValueChanged);
    delegateSpreadsheet.setSizeFull();
    add(delegateSpreadsheet);
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
            CellFunctions.asStringValue(cell).orElse(null)));
    delegateSpreadsheet.refreshCells(changedCells);
  }

  public void addRow(T rowData) {
    List<Cell> cells = createCellsForRow(rowData);
    delegateSpreadsheet.refreshCells(cells);

    rows.add(rowData);
    delegateSpreadsheet.setMaxRows(rows.size());
  }

  private List<Cell> createCellsForRow(T rowData) {
    int rowIndex = rows.size();
    List<Cell> createdCells = new ArrayList<>();
    for (Column<T, String> column : columns) {
      int colIndex = columns.indexOf(column);
      String cellValue = column.toCellValue.apply(rowData);
      Cell cell = delegateSpreadsheet.createCell(rowIndex, colIndex, cellValue);
      createdCells.add(cell);
    }
    return createdCells;
  }

  public void removeLastRow() {
    //TODO
  }

  public Column<T, String> addColumn(String name, Function<T, String> toCellValue,
      BiConsumer<T, String> modelEditor) {
    Column<T, String> column = new Column<>(name, toCellValue, modelEditor);
    columns.add(column);
    delegateSpreadsheet.refreshCells(createCellsForColumn(column));
    delegateSpreadsheet.setMaxColumns(columns.size());
    return column;
  }

  private List<Cell> createCellsForColumn(Column<T, String> column) {
    int colIndex = columns.indexOf(column);
    List<Cell> createdCells = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      String cellValue = column.toCellValue.apply(rows.get(rowIndex));
      Cell cell = delegateSpreadsheet.createCell(rowIndex, colIndex, cellValue);
      createdCells.add(cell);
    }
    return createdCells;
  }


  public List<T> getRows() {
    return rows;
  }

  public boolean isValid() {
    return valid;
  }

  public boolean isInvalid() {
    return !isValid();
  }

  public boolean validate() {
    //todo run validators

    return isValid();
  }

  private static class CellFunctions {

    static Optional<String> asStringValue(Cell cell) {
      return switch (cell.getCellType()) {
        case _NONE, FORMULA, ERROR -> Optional.empty();
        case NUMERIC -> Optional.of(String.valueOf(cell.getNumericCellValue()));
        case STRING -> Optional.of(cell.getStringCellValue());
        case BLANK -> Optional.of("");
        case BOOLEAN -> Optional.of(String.valueOf(cell.getBooleanCellValue()));
      };
    }
  }


  public static class Column<T, S> {

    private final String name;
    private final List<ColumnValidator<S>> validators;

    private Function<T, S> toCellValue;
    private BiConsumer<T, S> modelEditor;

    public Column(String name, Function<T, S> toCellValue, BiConsumer<T, S> modelEditor) {
      requireNonNull(name, "name must not be null");
      requireNonNull(toCellValue, "toCellValue must not be null");
      requireNonNull(modelEditor, "modelEditor must not be null");
      this.name = name;
      this.toCellValue = toCellValue;
      this.modelEditor = modelEditor;
      validators = new ArrayList<>();
    }

    public String getName() {
      return name;
    }

    List<ColumnValidator<S>> getValidators() {
      return Collections.unmodifiableList(validators);
    }

    public Column<T, S> withValidator(Predicate<S> predicate, String errorMessage) {
      validators.add(new ColumnValidator<>(predicate, errorMessage));
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

    public boolean validate(T2 value) {
      return predicate.test(value);
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }

}
