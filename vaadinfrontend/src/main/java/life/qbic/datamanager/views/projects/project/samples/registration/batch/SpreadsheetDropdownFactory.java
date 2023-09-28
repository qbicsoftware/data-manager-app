package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.component.spreadsheet.SpreadsheetComponentFactory;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.HashMap;
import java.util.List;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationSpreadsheet.SamplesheetHeaderName;
import life.qbic.projectmanagement.domain.project.sample.AnalysisMethod;
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

  private final HashMap<Integer, List<String>> columnIndexToCellValueOptions = new HashMap<>();

  // this method is not used atm, as we only want to show a component if a cell is selected for performance reasons
  @Override
  public Component getCustomComponentForCell(Cell cell, int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    return null;
  }

  @Override
  public Component getCustomEditorForCell(Cell cell,
      int rowIndex, int columnIndex,
      Spreadsheet spreadsheet, Sheet sheet) {
    //We only want to have a combobox if more than one value is selectable for the user and it's not the header row.
    if (hasMoreThanOneValue(columnIndex) && !isHeaderRow(rowIndex)) {
      ComboBox<String> editorCombobox = createEditorCombobox(spreadsheet, cell.getColumnIndex(),
          cell.getRowIndex());
      if (columnIndex == 1) {
        ComboBox<AnalysisMethod> combo = createEditorAnalysisComboBox(spreadsheet,
            cell.getColumnIndex(), cell.getRowIndex());
        if (!cell.getStringCellValue().isEmpty()) {
          combo.setValue(AnalysisMethod.forFixedTerm(cell.getStringCellValue().trim()));
        }
        return combo;
      }
      if (!cell.getStringCellValue().isEmpty()) {
        editorCombobox.setValue(cell.getStringCellValue());
      }
      return editorCombobox;
    }
    return null;
  }

  private ComboBox<AnalysisMethod> createEditorAnalysisComboBox(Spreadsheet spreadsheet,
      int selectedCellColumnIndex, int selectedCellRowIndex) {
    ComboBox<AnalysisMethod> editorComboBox = new ComboBox<>();
    editorComboBox.setClassName("spreadsheet-combo-box");
    editorComboBox.setItems(AnalysisMethod.values());
    editorComboBox.setRenderer(new ComponentRenderer<>(analysisMethod -> {
      var listItem = new Div();
      listItem.addClassName("spreadsheet-list-item");
      var label = new Div();
      label.setText(analysisMethod.label());
      var iconContainer = new Div();
      var questionMarkIcon = VaadinIcon.QUESTION_CIRCLE_O.create();
      questionMarkIcon.setTooltipText(analysisMethod.description());
      iconContainer.add(questionMarkIcon);

      listItem.add(label, iconContainer);
      return listItem;
    }));
    editorComboBox.setItemLabelGenerator(AnalysisMethod::term);
    editorComboBox.addValueChangeListener(valueChangeEvent -> {
      if (valueChangeEvent.isFromClient()) {
        //We add a whitespace so the value is not auto incremented when the user drags a value
        Cell createdCell = spreadsheet.createCell(selectedCellRowIndex, selectedCellColumnIndex,
            valueChangeEvent.getValue().term() + " ");
        spreadsheet.refreshCells(createdCell);
      }
    });
    return editorComboBox;
  }

  private ComboBox<String> createEditorCombobox(Spreadsheet spreadsheet,
      int selectedCellColumnIndex, int selectedCellRowIndex) {
    ComboBox<String> editorComboBox = new ComboBox<>();
    editorComboBox.setClassName("spreadsheet-combo-box");
    List<String> editorItems = getColumnValues(selectedCellColumnIndex);
    editorComboBox.setItems(editorItems);
    editorComboBox.addValueChangeListener(e -> {
      if (e.isFromClient()) {
        //We add a whitespace so the value is not auto incremented when the user drags a value
        Cell createdCell = spreadsheet.createCell(selectedCellRowIndex, selectedCellColumnIndex,
            e.getValue() + " ");
        spreadsheet.refreshCells(createdCell);
      }
    });
    return editorComboBox;
  }

  @Override
  public void onCustomEditorDisplayed(Cell cell, int rowIndex,
      int columnIndex, Spreadsheet spreadsheet,
      Sheet sheet, Component editor) {
  }

  private boolean isHeaderRow(int rowIndex) {
    return rowIndex == 0;
  }

  private boolean hasMoreThanOneValue(int columnIndex) {
    if (columnIndexToCellValueOptions.get(columnIndex) == null) {
      return false;
    }
    return columnIndexToCellValueOptions.get(columnIndex).size() > 1;
  }

  private List<String> getColumnValues(int columnIndex) {
    return columnIndexToCellValueOptions.get(columnIndex);
  }

  //We are only interested in the columnIndex to know which options should be shown in the editor component.
  public void setColumnValues(HashMap<SamplesheetHeaderName, List<String>> columnValues) {
    columnValues.forEach((samplesheetHeaderName, strings) -> columnIndexToCellValueOptions.put(
        samplesheetHeaderName.ordinal(), strings));
  }
}
