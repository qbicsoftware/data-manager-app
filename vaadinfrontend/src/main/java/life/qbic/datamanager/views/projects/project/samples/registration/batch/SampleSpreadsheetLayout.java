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
  public Spreadsheet sampleRegistrationSpreadsheet = new Spreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button registerButton = new Button("Register");
  private final SampleRegistrationSheetBuilder sampleRegistrationSheetBuilder;
  private final SampleMetadataLayoutHandler sampleInformationLayoutHandler;

  SampleSpreadsheetLayout(SampleRegistrationService sampleRegistrationService) {
    initContent();
    this.setSizeFull();
    sampleRegistrationSheetBuilder = new SampleRegistrationSheetBuilder(sampleRegistrationService);
    sampleInformationLayoutHandler = new SampleMetadataLayoutHandler();
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
      batchName.setText("");
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
      spreadsheet.setActiveSheetProtected("pwd");
      switch (metaDataTypes) {
        case PROTEOMICS -> addProteomicsSheet(spreadsheet);
        case LIGANDOMICS -> addLigandomicsSheet(spreadsheet);
        case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(spreadsheet);
        case METABOLOMICS -> addMetabolomicsSheet(spreadsheet);
      }
    }

    private void unlockColumn(Spreadsheet spreadsheet, int column, int minRow, int maxRow) {
      List<Cell> updatedCells = new ArrayList<Cell>();
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

      List<Cell> updatedCells = new ArrayList<Cell>();
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
      //TODO this should be known from experimental groups and sample size
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
