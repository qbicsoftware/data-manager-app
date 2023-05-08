package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.data.binder.Binder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.springframework.beans.factory.annotation.Autowired;

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

  SampleSpreadsheetLayout(SampleRegistrationService sampleRegistrationService, @Autowired ExperimentInformationService experimentInformationService) {
    initContent();
    this.setSizeFull();
    sampleRegistrationSheetBuilder = new SampleRegistrationSheetBuilder(sampleRegistrationService, experimentInformationService);
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
    private final ExperimentInformationService experimentInformationService;

    public SampleRegistrationSheetBuilder(SampleRegistrationService sampleRegistrationService,
        ExperimentInformationService experimentInformationService) {
      this.sampleRegistrationService = sampleRegistrationService;
      this.experimentInformationService = experimentInformationService;
    }

    public void addSheetToSpreadsheet(MetaDataTypes metaDataTypes, Spreadsheet spreadsheet) {
      spreadsheet.setActiveSheetProtected("pwd");
      switch (metaDataTypes) {
        case PROTEOMICS -> addProteomicsSheet(spreadsheet, sampleRegistrationService.retrieveProteomics());
        case LIGANDOMICS -> addLigandomicsSheet(spreadsheet, sampleRegistrationService.retrieveLigandomics());
        case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(spreadsheet, sampleRegistrationService.retrieveGenomics());
        case METABOLOMICS -> addMetabolomicsSheet(spreadsheet, sampleRegistrationService.retrieveMetabolomics());
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

    private void addProteomicsSheet(Spreadsheet spreadsheet, List<String> header) {
      setAndStyleHeader(spreadsheet, header);
      spreadsheet.reload();
      SpreadsheetDropdownFactory dropdownCellFactory = new SpreadsheetDropdownFactory();
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
    }

    private void addMetabolomicsSheet(Spreadsheet spreadsheet, List<String> header) {
      setAndStyleHeader(spreadsheet, header);
      spreadsheet.reload();
      SpreadsheetDropdownFactory dropdownCellFactory = new SpreadsheetDropdownFactory();
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
    }

    private void addLigandomicsSheet(Spreadsheet spreadsheet, List<String> header) {
      setAndStyleHeader(spreadsheet, header);
      spreadsheet.reload();
      SpreadsheetDropdownFactory dropdownCellFactory = new SpreadsheetDropdownFactory();
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
    }

    private void addGenomicsSheet(Spreadsheet spreadsheet, List<String> header) {
      setAndStyleHeader(spreadsheet, header);
      spreadsheet.reload();
      SpreadsheetDropdownFactory dropdownCellFactory = new SpreadsheetDropdownFactory();
      //List<ExperimentalGroupDTO> groups = experimentInformationService.getExperimentalGroups(
          //ExperimentId.parse("idbla"));
      int maximumNumberOfSamples = 10;//groups.stream().map(ExperimentalGroupDTO::sampleSize)
         // .mapToInt(Integer::valueOf).sum();
      DropDownColumn techColumn = new DropDownColumn().withItems(Arrays.asList("DNA-Seq", "RNA-Seq"));
      techColumn.fromRowIndex(1).toRowIndex(maximumNumberOfSamples+1).atColIndex(0);

      dropdownCellFactory.addDropdownColumn(techColumn);
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
    }

    /**
     * Creates and adds species and specimen Dropdown columns to a spreadsheet via a provided
     * SpreadsheetDropdownFactory dropdown column objects are only created if more than one species
     * or specimen is part of this experiment. Otherwise, the value is added directly to the
     * respective cells.
     */
    private void handleCommonMetadata(List<String> header, Spreadsheet spreadsheet,
        SpreadsheetDropdownFactory dropdownCellFactory) {
      int speciesColumn = header.indexOf("Species");
      int specimenColumn = header.indexOf("Specimen");

      /*
      List<String> species = experimentInformationService.getSpeciesOfExperiment(
          ExperimentId.parse("idbla")).stream().map(Species::value).toList();
      List<String> specimens = experimentInformationService.getSpecimensOfExperiment(
          ExperimentId.parse("idbla")).stream().map(Specimen::value).toList();
*/
      List<String> species = Arrays.asList("Canis lupus");
      List<String> specimens = Arrays.asList("Whole Blood", "Urine");
      int numberOfSamples = 100;

      List<Cell> updatedCells = new ArrayList<Cell>();

      if(species.size() == 1) {
        for (int rowIndex = 1; rowIndex <= numberOfSamples+1; rowIndex++) {
          Cell cell = spreadsheet.createCell(rowIndex, speciesColumn, species.get(0));
          updatedCells.add(cell);
        }
      } else {
        DropDownColumn speciesDropdown = new DropDownColumn().withItems(species);
        speciesDropdown.fromRowIndex(1).toRowIndex(numberOfSamples + 1).atColIndex(speciesColumn);
        dropdownCellFactory.addDropdownColumn(speciesDropdown);
      }
      if(specimens.size() == 1) {
        for (int rowIndex = 1; rowIndex <= numberOfSamples+1; rowIndex++) {
          Cell cell = spreadsheet.createCell(rowIndex, specimenColumn, specimens.get(0));
          updatedCells.add(cell);
        }
      } else {
        DropDownColumn specimenDropdown = new DropDownColumn().withItems(specimens);
        specimenDropdown.fromRowIndex(1).toRowIndex(numberOfSamples + 1).atColIndex(specimenColumn);
        dropdownCellFactory.addDropdownColumn(specimenDropdown);
      }
      spreadsheet.refreshCells(updatedCells);
    }

  }



}
