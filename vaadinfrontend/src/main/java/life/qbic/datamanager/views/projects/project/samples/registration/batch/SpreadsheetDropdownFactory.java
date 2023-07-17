package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * SpreadsheetDropdownFactory implements the SpreadsheetComponentFactory in order to style Spreadsheets to
 * contain cells with dropdown components. Information about dropdown values and which cells of the
 * spreadsheet should be styled that way must be provided.
 *
 * @since 1.0.0
 */
public class SpreadsheetDropdownFactory implements SpreadsheetComponentFactory {

  private List<DropdownColumn> dropdownColumns = new ArrayList<>();
  private Map<Cell, ComboBox> dropdownMap = new HashMap<>();

  /**
   * Initialises the dropdown factory to display a dropdown menu (ComboBox) in a specific column
   * @param column a DropDownColumn object specifying column index and items to be displayed
   */
  public void addDropdownColumn(DropdownColumn column) {
    this.dropdownColumns.add(column);
  }

  @Override
  public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    DropdownColumn dropDownColumn = findColumnInRange(rowIndex, columnIndex);
    if (spreadsheet.getActiveSheetIndex() == 0 && dropDownColumn!=null) {
      if (cell == null) {
        System.err.println("null");
        cell = spreadsheet.createCell(rowIndex, columnIndex, "");
      }
      if (cell.getCellStyle().getLocked()) {
        System.err.println("unlock");
        CellStyle unLockedStyle = spreadsheet.getWorkbook().createCellStyle();
        unLockedStyle.setLocked(false);
        cell.setCellStyle(unLockedStyle);
        spreadsheet.refreshCells(cell);
      }
      // if this cell contains a valid value, but no combobox (set via copying from other cells)
      // we create a dropdown with that value selected
      String value = cell.getStringCellValue();
      if(!value.isEmpty() && dropDownColumn.getItems().contains(value)) {
        ComboBox<String> comboBox = initCustomComboBox(dropDownColumn, rowIndex, columnIndex,
            spreadsheet);
        comboBox.setValue(value);
        dropdownMap.put(cell, comboBox);
        return comboBox;
      } else {
        if (dropdownMap.containsKey(cell)) {
          return dropdownMap.get(cell);
        } else {
          ComboBox<String> comboBox = initCustomComboBox(dropDownColumn, rowIndex, columnIndex,
              spreadsheet);
          dropdownMap.put(cell, comboBox);
          return comboBox;
        }
      }
    }
    return null;
  }

  // note: we need to unlock cells that will have an editor, otherwise "getCustomEditorForCell" is
  // not called
  @Override
  public Component getCustomEditorForCell(Cell cell,
      int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
/*
    DropdownColumn dropDownColumn = findColumnInRange(rowIndex, columnIndex);
    if(spreadsheet.getActiveSheetIndex()==0) {
      System.err.println("editor testing cell (" + rowIndex + ", " + columnIndex + ")");
      System.err.println(dropDownColumn);
    }
    if (spreadsheet.getActiveSheetIndex() == 0 && dropDownColumn!=null) {
      System.err.println("dropdown expected");
      List<String> dropdownItems = dropDownColumn.getItems();

      if (!dropdownItems.contains(SpreadsheetMethods.cellToStringOrNull(cell))) {
        return initCustomComboBox(dropDownColumn, rowIndex, columnIndex,
            spreadsheet);
      }
    }
    */

    return null;
  }

  private ComboBox<String> initCustomComboBox(DropdownColumn dropDownColumn, int rowIndex, int columnIndex,
      Spreadsheet spreadsheet) {
    List<String> items = dropDownColumn.getItems();
    ComboBox<String> comboBox = new ComboBox<>(dropDownColumn.getLabel(), items);

    comboBox.addValueChangeListener(e -> {
      Cell cell = spreadsheet.getCell(rowIndex, columnIndex);
      System.err.println("change in ("+rowIndex+", "+columnIndex+")");
      System.err.println(e.getValue());
      System.err.println(cell.getStringCellValue());
    });

    return comboBox;
  }

  @Override
  public void onCustomEditorDisplayed(Cell cell, int rowIndex,
      int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet, Component editor) {
    if(editor instanceof ComboBox) {
      System.err.println(((ComboBox) editor).getValue()+" selected.");
    }
    /* not implemented since no custom editor is currently used */
  }

  /**
   * Tests if a DropDownColumn has been defined for a provided column index and if it includes a
   * provided row, that is, if a cell is to be rendered as a dropdown. If yes, the DropDownColumn
   * object is returned, null otherwise.
   * @param rowIndex the row index of the spreadsheet cell to test
   * @param columnIndex the column index of the spreadsheet cell to test
   * @return the DropDownColumn object if it has been defined for the cell, null otherwise
   */
  public DropdownColumn findColumnInRange(int rowIndex, int columnIndex) {
    for(DropdownColumn dropDown : dropdownColumns) {
      if(dropDown.isWithinRange(rowIndex, columnIndex)) {
        return dropDown;
      }
    }
    return null;
  }

  /**
   * Increases rendering of a DropDownColumn in the specified column to include the specified row
   * Nothing happens if no DropDownColumn is defined for this column
   * @param rowIndex the row index of the spreadsheet cell
   * @param columnIndex the column index of the spreadsheet cell
   */
  public void addDropDownCell(int rowIndex, int columnIndex) {
    for(DropdownColumn dropDown : dropdownColumns) {
      if(dropDown.isInColumn(columnIndex)) {
        dropDown.increaseToRow(rowIndex);
      }
    }
  }

  /**
   * Returns a DropDownColumn defined for a specific column, irrespective of its row range. Returns
   * null if no DropDownColumn was defined.
   * @param columnIndex the spreadsheet column of the DropDownColumn
   * @return the DropDownColumn object if it has been defined at this index, null otherwise
   */
  public DropdownColumn getColumn(int columnIndex) {
    for(DropdownColumn dropDown : dropdownColumns) {
      if(dropDown.isInColumn(columnIndex)) {
        return dropDown;
      }
    }
    return null;
  }
}
