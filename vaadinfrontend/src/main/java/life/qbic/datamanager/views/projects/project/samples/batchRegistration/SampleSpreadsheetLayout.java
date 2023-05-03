package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.data.binder.Binder;
import java.util.ArrayList;
import java.util.List;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

/**
 * <b>Sample Spreadsheet Layout</b>
 * <p>
 * Layout which is responsible for hosting the spreadsheet in which the metadata information
 * associated for each sample will be provided>
 * </p>
 */
class SampleSpreadsheetLayout extends VerticalLayout {

  public Spreadsheet sampleRegistrationSpreadsheet = new Spreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button nextButton = new Button("Next");
  private final SampleRegistrationSheetBuilder sampleRegistrationSheetBuilder;
  private final SampleMetadataLayoutHandler sampleMetadataLayoutHandler;

  SampleSpreadsheetLayout(SampleRegistrationService sampleRegistrationService) {
    initContent();
    this.setSizeFull();
    sampleRegistrationSheetBuilder = new SampleRegistrationSheetBuilder(sampleRegistrationService);
    sampleMetadataLayoutHandler = new SampleMetadataLayoutHandler();
  }

  private void initContent() {
    add(sampleRegistrationSpreadsheet);
    styleSampleRegistrationSpreadSheet();
    initButtonLayout();
  }

  private void initButtonLayout() {
    HorizontalLayout sampleMetadataButtons = new HorizontalLayout();
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sampleMetadataButtons.add(cancelButton, nextButton);
    this.setAlignSelf(Alignment.END, sampleMetadataButtons);
    add(sampleMetadataButtons);
  }

  private void styleSampleRegistrationSpreadSheet() {
    sampleRegistrationSpreadsheet.setSizeFull();
    sampleRegistrationSpreadsheet.setSheetSelectionBarVisible(false);
    sampleRegistrationSpreadsheet.setFunctionBarVisible(false);
  }

  public void generateSampleRegistrationSheet(MetaDataTypes metaDataTypes) {
    sampleRegistrationSpreadsheet.reset();
    sampleRegistrationSheetBuilder.addSheetToSpreadsheet(metaDataTypes,
        sampleRegistrationSpreadsheet);
    sampleRegistrationSpreadsheet.reload();
  }

  public void reset() {
    sampleMetadataLayoutHandler.reset();
  }

  public boolean isInputValid() {
    return sampleMetadataLayoutHandler.isInputValid();
  }

  private class SampleMetadataLayoutHandler {

    private final List<Binder<?>> binders = new ArrayList<>();

    public SampleMetadataLayoutHandler() {
      configureValidators();
    }

    //ToDo add Binders for Cell Values in Spreadsheet
    private void configureValidators() {
    }

    private void reset() {
      resetChildValues();
      resetChildValidation();
    }

    private void resetChildValues() {
      sampleRegistrationSpreadsheet.reset();
      sampleRegistrationSpreadsheet.reload();
    }

    //ToDo reset Binder Validation State for each Cell
    private void resetChildValidation() {
    }

    private boolean isInputValid() {
      binders.forEach(Binder::validate);
      return binders.stream().allMatch(Binder::isValid);
    }
  }

  private static class SampleRegistrationSheetBuilder {

    private final SampleRegistrationService sampleRegistrationService;

    public SampleRegistrationSheetBuilder(SampleRegistrationService sampleRegistrationService) {
      this.sampleRegistrationService = sampleRegistrationService;
    }

    public void addSheetToSpreadsheet(MetaDataTypes metaDataTypes, Spreadsheet spreadsheet) {
      switch (metaDataTypes) {
        case PROTEOMICS -> addProteomicsSheet(spreadsheet);
        case LIGANDOMICS -> addLigandomicsSheet(spreadsheet);
        case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(spreadsheet);
        case METABOLOMICS -> addMetabolomicsSheet(spreadsheet);
      }
    }

    private void addProteomicsSheet(Spreadsheet spreadsheet) {
      List<String> proteomicsHeader = sampleRegistrationService.retrieveProteomics();
      CellStyle lockedCells = spreadsheet.getWorkbook().createCellStyle();
      Font font = spreadsheet.getWorkbook().createFont();
      font.setBold(true);
      //toDo Locking cells currently does not work
      lockedCells.setLocked(true);
      lockedCells.setFont(font);
      int columnIndex = 1;
      //toDo This currently only adds the header
      for (String columnHeader : proteomicsHeader) {
        Cell cell = spreadsheet.createCell(0, columnIndex, columnHeader);
        cell.setCellStyle(lockedCells);
        columnIndex++;
      }
      spreadsheet.setMaxColumns(proteomicsHeader.size());
      spreadsheet.reload();

    }

    private void addMetabolomicsSheet(Spreadsheet spreadsheet) {
      List<String> metabolomicsHeader = sampleRegistrationService.retrieveMetabolomics();
      CellStyle lockedCells = spreadsheet.getWorkbook().createCellStyle();
      Font font = spreadsheet.getWorkbook().createFont();
      font.setBold(true);
      //toDo Locking cells currently does not work
      lockedCells.setLocked(true);
      lockedCells.setFont(font);
      int columnIndex = 0;
      //toDo This currently only adds the header
      for (String columnHeader : metabolomicsHeader) {
        Cell cell = spreadsheet.createCell(0, columnIndex, columnHeader);
        cell.setCellStyle(lockedCells);
        columnIndex++;
      }
      spreadsheet.setMaxColumns(metabolomicsHeader.size());
      spreadsheet.reload();
    }

    private void addLigandomicsSheet(Spreadsheet spreadsheet) {
      List<String> ligandomicsHeader = sampleRegistrationService.retrieveLigandomics();
      CellStyle lockedCells = spreadsheet.getWorkbook().createCellStyle();
      Font font = spreadsheet.getWorkbook().createFont();
      font.setBold(true);
      //toDo Locking cells currently does not work
      lockedCells.setLocked(true);
      lockedCells.setFont(font);
      int columnIndex = 0;
      //toDo This currently only adds the header
      for (String columnHeader : ligandomicsHeader) {
        Cell cell = spreadsheet.createCell(0, columnIndex, columnHeader);
        cell.setCellStyle(lockedCells);
        spreadsheet.refreshCells(cell);
        columnIndex++;
      }
      spreadsheet.setMaxColumns(ligandomicsHeader.size());
      spreadsheet.reload();
    }

    private void addGenomicsSheet(Spreadsheet spreadsheet) {
      List<String> genomicsHeader = sampleRegistrationService.retrieveGenomics();
      CellStyle lockedCells = spreadsheet.getWorkbook().createCellStyle();
      Font font = spreadsheet.getWorkbook().createFont();
      font.setBold(true);
      //toDo Locking cells currently does not work
      lockedCells.setLocked(true);
      lockedCells.setFont(font);
      int columnIndex = 0;
      //toDo This currently only adds the header
      for (String columnHeader : genomicsHeader) {
        Cell cell = spreadsheet.createCell(0, columnIndex, columnHeader);
        cell.setCellStyle(lockedCells);
        columnIndex++;
      }
      spreadsheet.createFreezePane(1, 0);
      spreadsheet.setMaxColumns(genomicsHeader.size());
      spreadsheet.reload();
    }

  }

}
