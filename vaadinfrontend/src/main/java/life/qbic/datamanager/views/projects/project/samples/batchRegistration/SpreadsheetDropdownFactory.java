package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * SpreadsheetDropdownFactory implements the SpreadsheetComponentFactory in order to style Spreadsheets to
 * contain cells with dropdown components. Information about dropdown values and which cells of the
 * spreadsheet should be styled that way must be provided.
 *
 * @since 1.0.0
 */
public class SpreadsheetDropdownFactory implements SpreadsheetComponentFactory {

  List<DropDownColumn> dropDownColumns = new ArrayList<>();

  public void addDropdownColumn(DropDownColumn column) {
    this.dropDownColumns.add(column);
  }

  @Override
  public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet) {
    DropDownColumn dropDownColumn = findColumnInRange(rowIndex, columnIndex);

    if (spreadsheet.getActiveSheetIndex() == 0 && dropDownColumn!=null) {
      if(cell==null || cell.getStringCellValue().isEmpty()) {
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
    ComboBox analysisType = new ComboBox(dropDownColumn.getLabel(), dropDownColumn.getItems());
    analysisType.addValueChangeListener(e -> spreadsheet.refreshCells(
        spreadsheet.createCell(rowIndex, columnIndex, e.getValue())));
    return analysisType;
  }

  @Override
  public void onCustomEditorDisplayed(Cell cell, int rowIndex,
      int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet, Component editor) {
  }

  private DropDownColumn findColumnInRange(int rowIndex, int columnIndex) {
    for(DropDownColumn dropDown : dropDownColumns) {
      if(dropDown.isWithInRange(rowIndex, columnIndex)) {
        return dropDown;
      }
    }
    return null;
  }

}
