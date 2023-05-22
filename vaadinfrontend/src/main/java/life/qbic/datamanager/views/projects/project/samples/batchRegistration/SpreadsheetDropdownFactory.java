package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import java.util.ArrayList;
import java.util.List;
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
      List<String> dropdownItems = dropDownColumn.getItems();
      if(cell==null) {
        cell = spreadsheet.createCell(rowIndex, columnIndex, "");
      }
      if(cell.getCellStyle().getLocked()) {
        CellStyle unLockedStyle = spreadsheet.getWorkbook().createCellStyle();
        unLockedStyle.setLocked(false);
        cell.setCellStyle(unLockedStyle);
        spreadsheet.refreshCells(cell);
      }

      if(!dropdownItems.contains(cell.getStringCellValue())) {
        return initCustomComboBox(dropDownColumn, rowIndex, columnIndex,
            spreadsheet);
      }
    }
    return null;
  }

  @Override
  public Component getCustomEditorForCell(Cell cell,
      int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    return null;
  }

  private Component initCustomComboBox(DropdownColumn dropDownColumn, int rowIndex, int columnIndex,
      Spreadsheet spreadsheet) {
    List<String> items = dropDownColumn.getItems();
    ComboBox analysisType = new ComboBox(dropDownColumn.getLabel(), items);

    analysisType.addValueChangeListener(e -> {
      String newValue = (String) e.getValue();
      Cell cell = spreadsheet.getCell(rowIndex, columnIndex);
      cell.setCellValue(newValue);
      spreadsheet.refreshCells(cell);
    });
    return analysisType;
  }

  @Override
  public void onCustomEditorDisplayed(Cell cell, int rowIndex,
      int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet, Component editor) {
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
