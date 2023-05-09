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
import java.util.Set;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.domain.project.experiment.Condition;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.VariableName;
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
    public final String COL_SPACER = "___";

    public SampleRegistrationSheetBuilder(SampleRegistrationService sampleRegistrationService,
        ExperimentInformationService experimentInformationService) {
      this.sampleRegistrationService = sampleRegistrationService;
      this.experimentInformationService = experimentInformationService;
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

    private void addExperimentalVariableColumns(Spreadsheet spreadsheet,
        SpreadsheetDropdownFactory dropdownCellFactory) {

      /*
      List<String> species = experimentInformationService.getSpeciesOfExperiment(
          ExperimentId.parse("my experiment id")).stream().map(Species::value).toList();
      List<String> specimens = experimentInformationService.getSpecimensOfExperiment(
          ExperimentId.parse("my experiment id")).stream().map(Specimen::value).toList();
          List<ExperimentalGroupDTO> groups = experimentInformationService.getExperimentalGroups(
          ExperimentId.parse("my experiment id"));
*/

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
      //List<ExperimentalGroupDTO> groups = experimentInformationService.getExperimentalGroups(
          //ExperimentId.parse("my experiment id"));
      int maximumNumberOfSamples = 10;//groups.stream().map(ExperimentalGroupDTO::sampleSize)
         // .mapToInt(Integer::valueOf).sum();
      DropDownColumn techColumn = new DropDownColumn().withItems(Arrays.asList("DNA-Seq", "RNA-Seq"));
      techColumn.fromRowIndex(1).toRowIndex(maximumNumberOfSamples+1).atColIndex(0);

      dropdownCellFactory.addDropdownColumn(techColumn);
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
    }

    Condition createCondition(String variableName, String valueName, String valueUnit) {
      Condition condition;
      if (valueUnit != null) {
        condition = Condition.create(List.of(VariableLevel.create(VariableName.create(variableName), ExperimentalValue.create(valueName)), VariableLevel.create(VariableName.create(variableName), ExperimentalValue.create(valueName, valueUnit))));
      } else {
        condition = Condition.create(List.of(VariableLevel.create(VariableName.create(variableName), ExperimentalValue.create(valueName))));
      }
      return condition;
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
      int conditionColumn = header.indexOf("Condition");

      /*
      List<String> species = experimentInformationService.getSpeciesOfExperiment(
          ExperimentId.parse("my experiment id")).stream().map(Species::value).toList();
      List<String> specimens = experimentInformationService.getSpecimensOfExperiment(
          ExperimentId.parse("my experiment id")).stream().map(Specimen::value).toList();
          List<ExperimentalGroupDTO> groups = experimentInformationService.getExperimentalGroups(
          ExperimentId.parse("my experiment id"));
*/
      List<String> species = Arrays.asList("Canis lupus");
      List<String> specimens = Arrays.asList("Whole Blood", "Urine");
      int numberOfSamples = 100;

      // fix the width of dropdown columns
      String longestString = species.stream().max(Comparator.comparingInt(String::length)).get();
      Cell speciesCell = spreadsheet.createCell(1, speciesColumn, longestString+COL_SPACER);
      spreadsheet.autofitColumn(speciesColumn);

      longestString = specimens.stream().max(Comparator.comparingInt(String::length)).get();
      Cell specimenCell = spreadsheet.createCell(1, specimenColumn, longestString+COL_SPACER);
      spreadsheet.autofitColumn(specimenColumn);

      List<Cell> updatedCells = new ArrayList<Cell>();

      if(species.size() == 1) {
        for (int rowIndex = 1; rowIndex <= numberOfSamples+1; rowIndex++) {
          Cell cell = spreadsheet.createCell(rowIndex, speciesColumn, species.get(0));
          updatedCells.add(cell);
        }
      } else {
        DropDownColumn speciesDropdown = new DropDownColumn().withItems(species);
        speciesDropdown.toRowIndex(numberOfSamples + 1).atColIndex(speciesColumn);
        dropdownCellFactory.addDropdownColumn(speciesDropdown);
      }
      if(specimens.size() == 1) {
        for (int rowIndex = 1; rowIndex <= numberOfSamples+1; rowIndex++) {
          Cell cell = spreadsheet.createCell(rowIndex, specimenColumn, specimens.get(0));
          updatedCells.add(cell);
        }
      } else {
        DropDownColumn specimenDropdown = new DropDownColumn().withItems(specimens);
        specimenDropdown.toRowIndex(numberOfSamples + 1).atColIndex(specimenColumn);
        dropdownCellFactory.addDropdownColumn(specimenDropdown);
      }

      Condition test1 = Condition.create(List.of(VariableLevel.create(VariableName.create("Color"), ExperimentalValue.create("red")),
          VariableLevel.create(VariableName.create("Time"), ExperimentalValue.create("10", "Seconds"))));

      Condition test2 = Condition.create(List.of(VariableLevel.create(VariableName.create("Color"), ExperimentalValue.create("blue")),
          VariableLevel.create(VariableName.create("Time"), ExperimentalValue.create("10", "Seconds"))));

      ExperimentalGroup eGroup1 = ExperimentalGroup.create(test1, 12);
      ExperimentalGroup eGroup2 = ExperimentalGroup.create(test2, 8);

      List<ExperimentalGroupDTO> groups = new ArrayList<>();

      groups.add(new ExperimentalGroupDTO(eGroup1.condition().getVariableLevels(), eGroup1.sampleSize()));
      groups.add(new ExperimentalGroupDTO(eGroup2.condition().getVariableLevels(), eGroup2.sampleSize()));

      List<String> conditionItems = new ArrayList<>();

      // create condition items for dropdown and fix cell width
      for(ExperimentalGroupDTO group : groups) {
        List<String> varStrings = new ArrayList<>();
        for(VariableLevel level : group.levels()) {
          String varName = level.variableName().value();
          String value = level.experimentalValue().value();
          varStrings.add(varName+":"+value);
        }
        conditionItems.add(String.join("; ", varStrings));
      }
      longestString = conditionItems.stream().max(Comparator.comparingInt(String::length)).get();

      spreadsheet.createCell(1, conditionColumn, longestString+COL_SPACER+COL_SPACER);
      spreadsheet.autofitColumn(conditionColumn);

      DropDownColumn variableDropdown = new DropDownColumn().withItems(conditionItems);
      variableDropdown.toRowIndex(numberOfSamples + 1).atColIndex(conditionColumn);
      dropdownCellFactory.addDropdownColumn(variableDropdown);

      spreadsheet.refreshCells(updatedCells);
    }

  }



}
