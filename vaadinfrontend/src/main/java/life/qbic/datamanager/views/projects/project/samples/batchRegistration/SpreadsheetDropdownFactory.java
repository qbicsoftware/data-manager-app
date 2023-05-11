package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
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
  }

  @Override
  public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    DropDownColumn dropDownColumn = findColumnInRange(rowIndex, columnIndex);
    if (spreadsheet.getActiveSheetIndex() == 0 && dropDownColumn!=null) {
      List<String> dropdownItems = dropDownColumn.getItems();
      if(cell==null || !dropdownItems.contains(cell.getStringCellValue())) {
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
      CellStyle unLockedStyle = spreadsheet.getWorkbook().createCellStyle();
      unLockedStyle.setLocked(false);
      Cell cell = spreadsheet.createCell(rowIndex, columnIndex, newValue);
      cell.setCellStyle(unLockedStyle);
      spreadsheet.refreshCells(cell);
    });
    return analysisType;
  }

  private int countConditionsInColumn(Spreadsheet spreadsheet, DropDownColumn dropDownColumn, String condition, int column) {
    int res = 1;
    for(int row = 1; row < Integer.MAX_VALUE; row++) {
      if(!dropDownColumn.isWithInRange(row, column)) {
        break;
      }
      Cell cell = spreadsheet.getCell(row, column);
      if(cell!=null) {
        System.err.println(cell.getStringCellValue()+" counted");
      }
      if(cell!=null && condition.equals(cell.getStringCellValue())) {
        res++;
      }
    }
    System.err.println("condition: "+res);
    return res;
  }

  private void reportSampleSizeExceeded(String condition) {
    InformationMessage infoMessage = new InformationMessage("Sample size exceeded",
        "Group with condition: '"+condition+"' was selected for more samples than expected.");
    StyledNotification notification = new StyledNotification(infoMessage);
        notification.open();
  }

  @Override
  public void onCustomEditorDisplayed(Cell cell, int rowIndex,
      int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet, Component editor) {
  }

  public DropDownColumn findColumnInRange(int rowIndex, int columnIndex) {
    for(DropDownColumn dropDown : dropDownColumns) {
      if(dropDown.isWithInRange(rowIndex, columnIndex)) {
        return dropDown;
      }
    }
    return null;
  }

}
