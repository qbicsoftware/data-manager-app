package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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

  private final Span sampleInformationHeader = new Span("Sample Information");
  private final Span batchRegistrationInstruction = new Span();
  private final Label batchName = new Label();
  public final Spreadsheet sampleRegistrationSpreadsheet = new Spreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button registerButton = new Button("Register");
  private final SampleRegistrationSheetBuilder sampleRegistrationSheetBuilder;
  private final SampleInformationLayoutHandler sampleInformationLayoutHandler;

  SampleSpreadsheetLayout(SampleRegistrationService sampleRegistrationService) {
    initContent();
    this.setSizeFull();
    sampleRegistrationSheetBuilder = new SampleRegistrationSheetBuilder(sampleRegistrationService);
    sampleInformationLayoutHandler = new SampleInformationLayoutHandler();
  }

  private void initContent() {
    initHeaderAndInstruction();
    add(sampleRegistrationSpreadsheet);
    styleSampleRegistrationSpreadSheet();
    initButtonLayout();
  }

  private void initHeaderAndInstruction() {
    sampleInformationHeader.addClassNames("text-xl", "font-bold", "text-secondary");
    batchRegistrationInstruction.add("Please register your samples for Batch: ");
    batchRegistrationInstruction.add(batchName);
    batchName.addClassNames(FontWeight.BOLD, FontWeight.BLACK);
    add(sampleInformationHeader);
    add(batchRegistrationInstruction);
  }

  private void initButtonLayout() {
    HorizontalLayout sampleInformationButtons = new HorizontalLayout();
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sampleInformationButtons.add(cancelButton, registerButton);
    this.setAlignSelf(Alignment.END, sampleInformationButtons);
    add(sampleInformationButtons);
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
    sampleInformationLayoutHandler.reset();
  }

  public void setBatchName(String text) {
    batchName.setText(text);
  }

  public boolean isInputValid() {
    return sampleInformationLayoutHandler.isInputValid();
  }

  private class SampleInformationLayoutHandler implements Serializable {

    @Serial
    private static final long serialVersionUID = 2837608401189525502L;
    private final List<Binder<?>> binders = new ArrayList<>();

    private void reset() {
      resetChildValues();
    }

    private void resetChildValues() {
      batchName.setText("");
      sampleRegistrationSpreadsheet.reset();
      sampleRegistrationSpreadsheet.reload();
    }

    private boolean isInputValid() {
      binders.forEach(Binder::validate);
      return binders.stream().allMatch(Binder::isValid);
    }
  }

  private static class SampleRegistrationSheetBuilder implements Serializable {

    @Serial
    private static final long serialVersionUID = 573778360298068552L;
    private final transient SampleRegistrationService sampleRegistrationService;

    public SampleRegistrationSheetBuilder(SampleRegistrationService sampleRegistrationService) {
      this.sampleRegistrationService = sampleRegistrationService;
    }

    public void addSheetToSpreadsheet(MetaDataTypes metaDataTypes, Spreadsheet spreadsheet) {
      spreadsheet.setActiveSheetProtected("pwd");
      switch (metaDataTypes) {
        case PROTEOMICS -> addProteomicsSheet(spreadsheet);
        case LIGANDOMICS -> addLigandomicsSheet(spreadsheet);
        case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(spreadsheet);
        case METABOLOMICS -> addMetabolomicsSheet(spreadsheet);
      }
    }

    private void unlockColumn(Spreadsheet spreadsheet, int column, int minRow, int maxRow) {
      List<Cell> updatedCells = new ArrayList<>();
      CellStyle unLockedStyle = spreadsheet.getWorkbook().createCellStyle();
      unLockedStyle.setLocked(false);
      for (int i = minRow; i <= maxRow; i++) {
        Cell cell = spreadsheet.createCell(i, column, "");
        cell.setCellStyle(unLockedStyle);
      }
      spreadsheet.refreshCells(updatedCells);
    }

    private void setAndStyleHeader(Spreadsheet spreadsheet, List<String> header) {
      CellStyle boldHeaderStyle = spreadsheet.getWorkbook().createCellStyle();
      Font font = spreadsheet.getWorkbook().createFont();
      font.setBold(true);
      boldHeaderStyle.setFont(font);

      List<Cell> updatedCells = new ArrayList<>();
      int columnIndex = 0;
      for (String columnHeader : header) {
        String extraSpacer = "___";
        Cell cell = spreadsheet.createCell(0, columnIndex, columnHeader+extraSpacer);
        cell.setCellStyle(boldHeaderStyle);
        spreadsheet.autofitColumn(columnIndex);
        cell.setCellValue(columnHeader);
        columnIndex++;

      }
      spreadsheet.refreshCells(updatedCells);
    }

    private void addProteomicsSheet(Spreadsheet spreadsheet) {
      setAndStyleHeader(spreadsheet, sampleRegistrationService.retrieveProteomics());
      spreadsheet.reload();
    }

    private void addMetabolomicsSheet(Spreadsheet spreadsheet) {
      setAndStyleHeader(spreadsheet, sampleRegistrationService.retrieveMetabolomics());
      spreadsheet.reload();
    }

    private void addLigandomicsSheet(Spreadsheet spreadsheet) {
      setAndStyleHeader(spreadsheet, sampleRegistrationService.retrieveLigandomics());
      spreadsheet.reload();
    }

    private void addGenomicsSheet(Spreadsheet spreadsheet) {
      setAndStyleHeader(spreadsheet, sampleRegistrationService.retrieveGenomics());
      spreadsheet.reload();
      SpreadsheetDropdownFactory dropdownCellFactory = new SpreadsheetDropdownFactory();
      int maximumNumberOfSamples = 100;
      int firstDataRow = 1;
      int dropDownColumn = 0;
      unlockColumn(spreadsheet, dropDownColumn, firstDataRow, maximumNumberOfSamples+firstDataRow);
      dropdownCellFactory.fromColIndex(dropDownColumn).toColIndex(dropDownColumn);
      dropdownCellFactory.fromRowIndex(1).toRowIndex(maximumNumberOfSamples+firstDataRow);
      dropdownCellFactory.withItems(Arrays.asList("DNA-Seq", "RNA-Seq"));
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
    }

  }



}
