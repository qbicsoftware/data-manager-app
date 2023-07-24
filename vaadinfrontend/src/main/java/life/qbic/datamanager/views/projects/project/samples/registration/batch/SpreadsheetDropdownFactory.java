package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import java.util.HashMap;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * SpreadsheetDropdownFactory implements the SpreadsheetComponentFactory in order to style
 * Spreadsheets to contain cells with dropdown components. Information about dropdown values and
 * which cells of the spreadsheet should be styled that way must be provided.
 *
 * @since 1.0.0
 */
public class SpreadsheetDropdownFactory implements SpreadsheetComponentFactory {

  private final HashMap<Integer, List<String>> colIndexToComboBoxItems = new HashMap<>();

  @Override
  public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    return null;
  }

  // note: we need to unlock cells in addition to returning null in getCustomComponentForCell,
  // otherwise "getCustomEditorForCell" is not called
  // this method is not used atm, as it only shows the component when the cell is selected
  @Override
  public Component getCustomEditorForCell(Cell cell,
      int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    if (isHeaderRow(rowIndex)) {
      return null;
    }
    if (hasMoreThanOneValue(columnIndex)) {
      ComboBox<String> editorCombobox = createEditorCombobox(spreadsheet, cell.getColumnIndex(),
          cell.getRowIndex());
      editorCombobox.setValue(cell.getStringCellValue());
      return editorCombobox;
    }
    return null;
  }

  private ComboBox<String> createEditorCombobox(Spreadsheet spreadsheet,
      int selectedCellColumnIndex, int selectedCellRowIndex) {
    ComboBox<String> editorComboBox = new ComboBox<>();
    List<String> editorItems = getColumnValues(selectedCellColumnIndex);
    editorComboBox.setItems(editorItems);
    editorComboBox.addValueChangeListener(e -> {
      if (e.isFromClient()) {
        Cell createdCell = spreadsheet.createCell(selectedCellRowIndex, selectedCellColumnIndex,
            e.getValue());
        spreadsheet.refreshCells(createdCell);
      }
    });
    return editorComboBox;
  }

  private boolean isHeaderRow(int rowIndex) {
    return rowIndex == 1;
  }

  private boolean hasMoreThanOneValue(int columnIndex) {
    if (colIndexToComboBoxItems.get(columnIndex) == null) {
      return false;
    }
    return colIndexToComboBoxItems.get(columnIndex).size() > 1;
  }

  @Override
  public void onCustomEditorDisplayed(Cell cell, int rowIndex,
      int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet, Component editor) {

  }

  private List<String> getColumnValues(int columnIndex) {
    return colIndexToComboBoxItems.get(columnIndex);
  }

  public void setColumnValues(HashMap<Integer, List<String>> columnValues) {
    colIndexToComboBoxItems.putAll(columnValues);
  }
}
