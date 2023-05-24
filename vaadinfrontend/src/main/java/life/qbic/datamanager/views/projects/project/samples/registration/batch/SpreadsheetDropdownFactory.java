package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
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

  private String dropDownLabel = "";

  private List<String> dropdownItems;
  private int fromRowIndex = 0;
  private int fromColIndex = 0;
  private int toRowIndex = 1000;
  private int toColIndex = 1000;

  public SpreadsheetDropdownFactory withDropdownLabel(String label) {
    this.dropDownLabel = label;
    return this;
  }

  public SpreadsheetDropdownFactory withItems(List<String> items) {
    this.dropdownItems = items;
    return this;
  }

  public SpreadsheetDropdownFactory fromRowIndex(int i) {
    this.fromRowIndex = i;
    return this;
  }

  public SpreadsheetDropdownFactory toRowIndex(int i) {
    this.toRowIndex = i;
    return this;
  }

  public SpreadsheetDropdownFactory fromColIndex(int i) {
    this.fromColIndex = i;
    return this;
  }

  public SpreadsheetDropdownFactory toColIndex(int i) {
    this.toColIndex = i;
    return this;
  }

  @Override
  public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    if (spreadsheet.getActiveSheetIndex() == 0
        && rowIndex >= fromRowIndex && rowIndex <= toRowIndex && columnIndex >= fromColIndex
        && columnIndex <= toColIndex) {
      if(cell==null || !dropdownItems.contains(cell.getStringCellValue())) {
        return initCustomComboBox(rowIndex, columnIndex, spreadsheet);
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

  private Component initCustomComboBox(int rowIndex, int columnIndex, Spreadsheet spreadsheet) {
    ComboBox analysisType = new ComboBox(dropDownLabel, dropdownItems);
    analysisType.addValueChangeListener(e -> spreadsheet.refreshCells(
        spreadsheet.createCell(rowIndex, columnIndex, e.getValue())));
    return analysisType;
  }

  @Override
  public void onCustomEditorDisplayed(Cell cell, int rowIndex,
      int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet, Component editor) {
  }

}
