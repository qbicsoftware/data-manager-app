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

  private List<DropDownColumn> dropDownColumns = new ArrayList<>();

  public void addDropdownColumn(DropDownColumn column) {
    this.dropDownColumns.add(column);
    for(String item : column.getItems()) {
      System.err.println("x"+item+"x");
    }
  }

  @Override
  public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    DropDownColumn dropDownColumn = findColumnInRange(rowIndex, columnIndex);
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
        System.err.println(cell.getStringCellValue()+" not in "+dropdownItems);
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

  private Component initCustomComboBox(DropDownColumn dropDownColumn, int rowIndex, int columnIndex,
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

  public DropDownColumn findColumnInRange(int rowIndex, int columnIndex) {
    for(DropDownColumn dropDown : dropDownColumns) {
      if(dropDown.isWithinRange(rowIndex, columnIndex)) {
        return dropDown;
      }
    }
    return null;
  }

}
