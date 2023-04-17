package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import java.util.List;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
class SampleMetadataLayout extends VerticalLayout {

  public Spreadsheet sampleRegistrationSpreadsheet = new Spreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button nextButton = new Button("Next");
  private final SampleRegistrationSheetBuilder sampleRegistrationSheetBuilder;

  SampleMetadataLayout(SampleRegistrationService sampleRegistrationService) {
    initContent();
    this.setSizeFull();
    sampleRegistrationSheetBuilder = new SampleRegistrationSheetBuilder(sampleRegistrationService);
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
    sampleRegistrationSpreadsheet.reset();
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
        case TRANSCRIPTOMIC_GENOMICS -> addGenomicsSheet(spreadsheet);
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
