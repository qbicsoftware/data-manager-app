package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.data.binder.Binder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
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

  public void setActiveExperiment(Experiment experiment) {
    SampleRegistrationSheetBuilder.setActiveExperiment(experiment);
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
    public final String COL_SPACER = "___";
    private static Experiment activeExperiment;

    public SampleRegistrationSheetBuilder(SampleRegistrationService sampleRegistrationService) {
      this.sampleRegistrationService = sampleRegistrationService;
    }

    public static void setActiveExperiment(Experiment experiment) {
      activeExperiment = experiment;
    }

    public void addSheetToSpreadsheet(MetaDataTypes metaDataTypes, Spreadsheet spreadsheet) {
      spreadsheet.setActiveSheetProtected("password-needed-to-lock");
      SpreadsheetDropdownFactory dropdownCellFactory = new SpreadsheetDropdownFactory();
      switch (metaDataTypes) {
        case PROTEOMICS -> addProteomicsSheet(spreadsheet, sampleRegistrationService.retrieveProteomics(), dropdownCellFactory);
        case LIGANDOMICS -> addLigandomicsSheet(spreadsheet, sampleRegistrationService.retrieveLigandomics(), dropdownCellFactory);
        case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(spreadsheet, sampleRegistrationService.retrieveGenomics(), dropdownCellFactory);
        case METABOLOMICS -> addMetabolomicsSheet(spreadsheet, sampleRegistrationService.retrieveMetabolomics(), dropdownCellFactory);
      }
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
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
        Cell cell = spreadsheet.createCell(0, columnIndex, columnHeader+COL_SPACER);
        cell.setCellStyle(boldHeaderStyle);
        spreadsheet.autofitColumn(columnIndex);
        cell.setCellValue(columnHeader);
        columnIndex++;

      }
      spreadsheet.refreshCells(updatedCells);
    }

    private void addProteomicsSheet(Spreadsheet spreadsheet, List<String> header,
        SpreadsheetDropdownFactory dropdownCellFactory) {
      setAndStyleHeader(spreadsheet, header);
      spreadsheet.reload();
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
    }

    private void addMetabolomicsSheet(Spreadsheet spreadsheet, List<String> header,
        SpreadsheetDropdownFactory dropdownCellFactory) {
      setAndStyleHeader(spreadsheet, header);
      spreadsheet.reload();
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
    }

    private void addLigandomicsSheet(Spreadsheet spreadsheet, List<String> header,
        SpreadsheetDropdownFactory dropdownCellFactory) {
      setAndStyleHeader(spreadsheet, header);
      spreadsheet.reload();
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
    }

    private void addGenomicsSheet(Spreadsheet spreadsheet, List<String> header,
        SpreadsheetDropdownFactory dropdownCellFactory) {
      setAndStyleHeader(spreadsheet, header);
      spreadsheet.reload();
      List<ExperimentalGroup> groups = activeExperiment.getExperimentalGroups().stream().toList();
      int numberOfSamples = groups.stream().map(ExperimentalGroup::sampleSize).mapToInt(Integer::intValue).sum();

      DropDownColumn techColumn = new DropDownColumn().withItems(Arrays.asList("DNA-Seq", "RNA-Seq"));
      techColumn.fromRowIndex(1).toRowIndex(numberOfSamples).atColIndex(0);

      dropdownCellFactory.addDropdownColumn(techColumn);
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
    }

    //TODO check for max group size
    /*
    InformationMessage successMessage = new InformationMessage("No experimental variables are defined",
        "Please define all of your experimental variables before adding groups.");
    StyledNotification notification = new StyledNotification(successMessage);
        notification.open();
        */

    /**
     * Creates and adds species and specimen Dropdown columns to a spreadsheet via a provided
     * SpreadsheetDropdownFactory dropdown column objects are only created if more than one species
     * or specimen is part of this experiment. Otherwise, the value is added directly to the
     * respective cells.
     */
    private void handleCommonMetadata(List<String> header, Spreadsheet spreadsheet,
        SpreadsheetDropdownFactory dropdownCellFactory) {

      List<ExperimentalGroup> groups = activeExperiment.getExperimentalGroups().stream().toList();
      int numberOfSamples = groups.stream().map(ExperimentalGroup::sampleSize).mapToInt(Integer::intValue).sum();

      List<String> species = activeExperiment.getSpecies().stream().map(Species::label).toList();
      List<String> specimens = activeExperiment.getSpecimens().stream().map(Specimen::label).toList();

      fillSampleSourceCells(header.indexOf("Species"), species, spreadsheet, dropdownCellFactory, numberOfSamples);
      fillSampleSourceCells(header.indexOf("Specimen"), specimens, spreadsheet, dropdownCellFactory, numberOfSamples);

      List<String> conditionItems = new ArrayList<>();

      // create condition items for dropdown and fix cell width
      for(ExperimentalGroup group : groups) {
        List<String> varStrings = new ArrayList<>();
        for(VariableLevel level : group.condition().getVariableLevels()) {
          String varName = level.variableName().value();
          String value = level.experimentalValue().value();
          varStrings.add(varName+":"+value);
        }
        conditionItems.add(String.join("; ", varStrings));
      }
      String longestString = conditionItems.stream().max(Comparator.comparingInt(String::length)).get();
      int conditionColumn = header.indexOf("Condition");

      spreadsheet.createCell(1, conditionColumn, longestString+COL_SPACER+COL_SPACER);
      spreadsheet.autofitColumn(conditionColumn);

      DropDownColumn variableDropdown = new DropDownColumn().withItems(conditionItems);
      variableDropdown.toRowIndex(numberOfSamples).atColIndex(conditionColumn);
      dropdownCellFactory.addDropdownColumn(variableDropdown);

    }

    private void fillSampleSourceCells(int colIndex, List<String> items, Spreadsheet spreadsheet,
        SpreadsheetDropdownFactory dropdownCellFactory, int numberOfSamples) {

      // fix the width of dropdown columns
      String longestString = items.stream().max(Comparator.comparingInt(String::length)).get();
      Cell speciesCell = spreadsheet.createCell(1, colIndex, longestString+COL_SPACER);
      spreadsheet.autofitColumn(colIndex);

      List<Cell> updatedCells = new ArrayList<>();

      if(items.size() == 1) {
        for (int rowIndex = 1; rowIndex <= numberOfSamples; rowIndex++) {
          Cell cell = spreadsheet.createCell(rowIndex, colIndex, items.get(0));
          updatedCells.add(cell);
        }
      } else {
        DropDownColumn itemDropdown = new DropDownColumn().withItems(items);
        itemDropdown.toRowIndex(numberOfSamples).atColIndex(colIndex);
        dropdownCellFactory.addDropdownColumn(itemDropdown);
      }
      spreadsheet.refreshCells(updatedCells);
    }

  }



}
