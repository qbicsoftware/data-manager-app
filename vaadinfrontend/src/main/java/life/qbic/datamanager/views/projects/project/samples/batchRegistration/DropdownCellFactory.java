package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * DropdownCellFactory implements the SpreadsheetComponentFactory in order to style Spreadsheets to
 * contain cells with dropdown components. Information about dropdown values and which cells of the
 * spreadsheet should be styled that way must be provided.
 *
 * @since 1.0.0
 */
public class DropdownCellFactory implements SpreadsheetComponentFactory {

  private List<String> dropdownItems;
  private int fromRowIndex;
  private int fromColIndex;
  private int toRowIndex;
  private int toColIndex;

  @Override
  public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet) {
    if (spreadsheet.getActiveSheetIndex() == 0
        && rowIndex > 0 && rowIndex < 100 && columnIndex == 0) {
      String value = cell.getStringCellValue();
      if(value.isEmpty()) {
        return initCustomComboBox(rowIndex, columnIndex,
            spreadsheet);
      }
    }
    return null;
  }

  @Override
  public Component getCustomEditorForCell(Cell cell,
      int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
        /*
        //This method is not necessary for this functionality atm
        if (spreadsheet.getActiveSheetIndex() == 0
            && rowIndex > 0 && rowIndex < 100 && columnIndex == 0) {
          String value = cell.getStringCellValue();
          System.err.println("editor "+value);
          if(value.isEmpty()) {
            return initCustomComboBox(rowIndex, columnIndex,
                spreadsheet);
          }
        }*/
    return null;
  }

  private Component initCustomComboBox(int rowIndex, int columnIndex,
      Spreadsheet spreadsheet) {

    ComboBox analysisType = new ComboBox("", "RNA-Seq","DNA-Seq");
    analysisType.addValueChangeListener(e -> spreadsheet.refreshCells(
        spreadsheet.createCell(rowIndex, columnIndex, e.getValue())));
    return analysisType;
  }

  @Override
  public void onCustomEditorDisplayed(Cell cell, int rowIndex,
      int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet, Component editor) {
        /*
        //This method is not necessary for this functionality atm
        System.err.println("custom editor displayed");
        if (cell == null) {
          return;
        }
        ((ComboBox) editor)
            .setValue(cell.getStringCellValue());

         */
  }

}
