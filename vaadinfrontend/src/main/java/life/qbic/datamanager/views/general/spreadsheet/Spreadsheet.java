package life.qbic.datamanager.views.general.spreadsheet;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.spreadsheet.Spreadsheet.CellValueChangeEvent;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.Column.ColumnValidator.ValidationResult;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.Column.SelectEditor;
import life.qbic.logging.api.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class Spreadsheet<T> extends Div {

  private static final Logger log = logger(Spreadsheet.class);

  private boolean valid = true;

  private final com.vaadin.flow.component.spreadsheet.Spreadsheet delegateSpreadsheet = new com.vaadin.flow.component.spreadsheet.Spreadsheet();
  private final List<Column<T>> columns = new ArrayList<>();
  private final List<T> rows = new ArrayList<>();

  public Spreadsheet() {
    setMinHeight("50vh"); //fixme
    addClassName("spreadsheet-container");
    delegateSpreadsheet.setSheetSelectionBarVisible(false);
    delegateSpreadsheet.setFunctionBarVisible(false);
    delegateSpreadsheet.setRowColHeadingsVisible(false);
    delegateSpreadsheet.addCellValueChangeListener(this::onCellValueChanged);
    delegateSpreadsheet.setSizeFull();
    delegateSpreadsheet.setSpreadsheetComponentFactory(new MyComponentFactory());
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
    delegateSpreadsheet.setMaxRows(rowCount());
  }

  private List<Cell> createCellsForRow(T rowData) {
    int rowIndex = rowCount();
    List<Cell> createdCells = new ArrayList<>();
    for (Column<T> column : columns) {
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

  public Column<T> addColumn(String name, Function<T, String> toCellValue,
      BiConsumer<T, String> modelEditor) {
    Column<T> column = new Column<>(name, toCellValue, modelEditor);
    columns.add(column);
    delegateSpreadsheet.refreshCells(createCellsForColumn(column));
    delegateSpreadsheet.setMaxColumns(columnCount());
    return column;
  }

  private List<Cell> createCellsForColumn(Column<T> column) {
    int colIndex = columns.indexOf(column);
    List<Cell> createdCells = new ArrayList<>();
    for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
      String cellValue = column.toCellValue.apply(rows.get(rowIndex));
      Cell cell = delegateSpreadsheet.createCell(rowIndex, colIndex, cellValue);
      createdCells.add(cell);
    }
    return createdCells;
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
    return valid;
  }

  public boolean isInvalid() {
    return !isValid();
  }

  public ValidationResult validate() {
    for (int rowIndex = 0; rowIndex < rowCount(); rowIndex++) {
      for (int colIndex = 0; colIndex < columnCount(); colIndex++) {
        Column<T> column = columns.get(colIndex);
        Cell cell = delegateSpreadsheet.getCell(rowIndex, colIndex);
        Optional<ValidationResult> failingValidator = column.getValidators().stream()
            .map(it -> it.validate(CellFunctions.asStringValue(cell)
                .orElse(null)))
            .peek(System.out::println)
            .filter(ValidationResult::isInvalid)
            .findFirst();
        if (failingValidator.isPresent()) {
          ValidationResult validationResult = failingValidator.get();
          valid = validationResult.isValid();
          return validationResult;
        }
      }
    }
    return new ValidationResult(true, "");
  }

  private List<Cell> getRow(int index) {
    if (index >= rowCount()) {
      throw new IllegalArgumentException(
          "There is no row at index " + index + ". The maximum index is " + rowCount());
    }
    List<Cell> cells = new ArrayList<>();
    for (int colIndex = 0; colIndex < columnCount(); colIndex++) {
      Column<T> column = columns.get(colIndex);
      cells.add(delegateSpreadsheet.getCell(index, colIndex));
    }
    return cells;
  }

  private void updateCell(int rowIndex, int colIndex, String value) {
    Cell cell =
        delegateSpreadsheet.getCell(rowIndex, colIndex) == null ? delegateSpreadsheet.createCell(
            rowIndex, colIndex, null) : delegateSpreadsheet.getCell(rowIndex, colIndex);
    updateCell(cell, value);
  }

  private void updateCell(Cell cell, String value) {
    CellFunctions.setCellValue(cell, value);
    onCellValueChanged(new CellValueChangeEvent(delegateSpreadsheet,
        Set.of(new CellReference(cell.getRowIndex(), cell.getColumnIndex()))));
  }


  private static class CellFunctions {

    static Optional<String> asStringValue(Cell cell) {
      return switch (cell.getCellType()) {
        case _NONE, FORMULA, ERROR, BLANK -> Optional.empty();
        case NUMERIC -> Optional.of(String.valueOf(cell.getNumericCellValue()));
        case STRING -> Optional.of(cell.getStringCellValue());
        case BOOLEAN -> Optional.of(String.valueOf(cell.getBooleanCellValue()));
      };
    }

    static void setCellValue(Cell cell, String value) {
      switch (cell.getCellType()) {
        case _NONE -> {
        }
        case NUMERIC -> {
          cell.setCellValue(Double.parseDouble(value));
        }
        case STRING -> {
          cell.setCellValue(value);
        }
        case FORMULA -> {
        }
        case BLANK -> {
          cell.setCellValue(value);
        }
        case BOOLEAN -> {
          cell.setCellValue(Boolean.parseBoolean(value));
        }
        case ERROR -> {
        }
      }
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
          selectEditor.setFromCellValue(CellFunctions.asStringValue(cell).orElse(null));

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
      withValidator(value -> Objects.nonNull(value) || !this.isRequired(),
          "The column " + getName() + " does not allow empty values. Please enter a value.");
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

    public Column<T> setRequired(boolean required) {
      this.required = required;
      return this;
    }
    public <E> Column<T> selectFrom(List<E> values, Function<E, String> toCellValue,
        ComponentRenderer<? extends Component, E> renderer) {
      List<String> possibleCellValues = values.stream()
          .map(toCellValue).toList();
      this.withValidator(value -> isNull(value)
              || possibleCellValues.stream().anyMatch(it -> it.equals(value)),
          "{0} is not a valid option. Please choose from %s".formatted(possibleCellValues));
      SelectEditor<E> selectEditor = new SelectEditor<>(values, toCellValue);
      selectEditor.setRenderer(renderer);
      selectEditor.setItemLabelGenerator(toCellValue::apply);
      this.editorComponent = selectEditor;
      return this;
    }

    public interface CellEditor {

      void removeAllValueChangeListeners();
    }

    public static class SelectEditor<E> extends Select<E> implements CellEditor {

      private final List<Registration> addedValueChangeListeners;

      private final Function<E, String> toCellValue;

      public SelectEditor(List<E> items, Function<E, String> toCellValue) {
        addedValueChangeListeners = new ArrayList<>();
        setItems(items);
        this.toCellValue = toCellValue;
      }

      public String toCellValue(E value) {
        if (Objects.isNull(value)) {
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

      @Override
      public void removeAllValueChangeListeners() {
        addedValueChangeListeners.forEach(Registration::remove);
        addedValueChangeListeners.clear();
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
        return new ValidationResult(isValid, filledErrorMessage);
      }

      public record ValidationResult(boolean isValid, String errorMessage) {

        public ValidationResult {
          if (isValid) {
            errorMessage = "";
          }
        }

        public boolean isInvalid() {
          return !isValid();
        }
      }
    }
  }
}
