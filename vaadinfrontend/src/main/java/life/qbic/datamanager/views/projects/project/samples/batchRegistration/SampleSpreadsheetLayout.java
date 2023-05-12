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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;

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

  public List<NGSRowDTO> getFilledRows(List<String> header) {
    List<NGSRowDTO> rows = new ArrayList<>();
    for(int i = 1; i < Integer.MAX_VALUE; i++) {
      Row row = sampleRegistrationSpreadsheet.getActiveSheet().getRow(i);
      Cell analysisTypeCell = row.getCell(header.indexOf("Analysis to be performed"));
      Cell sampleLabelCell = row.getCell(header.indexOf("Sample label"));
      Cell replicateIDCell = row.getCell(header.indexOf("Biological replicate id"));
      Cell conditionCell = row.getCell(header.indexOf("Condition"));
      Cell speciesCell = row.getCell(header.indexOf("Species"));
      Cell specimenCell = row.getCell(header.indexOf("Specimen"));
      Cell commentCell = row.getCell(header.indexOf("Customer comment"));

      Stream<Cell> mandatoryCellStream = Stream.of(analysisTypeCell, sampleLabelCell,
          replicateIDCell, conditionCell, speciesCell, specimenCell);

      if (mandatoryCellStream.anyMatch(x -> x == null)) {
        break;
      }
      if(mandatoryCellStream.noneMatch(x -> x.getStringCellValue().isEmpty())) {
        rows.add(new NGSRowDTO(analysisTypeCell.getStringCellValue().trim(), sampleLabelCell.getStringCellValue().trim(),
            replicateIDCell.getStringCellValue().trim(), conditionCell.getStringCellValue().trim(),
            speciesCell.getStringCellValue().trim(), specimenCell.getStringCellValue().trim(), commentCell.getStringCellValue().trim()));
      }

      if(analysisTypeCell==null) {
        break;
      }
    }
    return rows;
  }

  public record NGSRowDTO(String analysisType, String sampleLabel, String bioReplicateID,
                          String condition, String species, String specimen,
                          String customerComment) {
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
    private static Experiment activeExperiment;

    private static int numberOfSamples;

    public SampleRegistrationSheetBuilder(SampleRegistrationService sampleRegistrationService) {
      this.sampleRegistrationService = sampleRegistrationService;
    }

    public static void setActiveExperiment(Experiment experiment) {
      activeExperiment = experiment;
      List<ExperimentalGroup> groups = activeExperiment.getExperimentalGroups().stream().toList();
      numberOfSamples = 3*groups.stream().map(ExperimentalGroup::sampleSize)
          .mapToInt(Integer::intValue).sum();
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
      unlockEmptyColumns(spreadsheet, dropdownCellFactory);
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
    }

    private void unlockEmptyColumns(Spreadsheet spreadsheet, SpreadsheetDropdownFactory dropdownCellFactory) {
      for(int column = 0; column < Integer.MAX_VALUE; column++) {
        Cell firstCell = spreadsheet.getCell(0, column);
        Cell firstDataCell = spreadsheet.getCell(1, column);
        boolean hasHeader = !isCellEmpty(firstCell);
        if(!hasHeader) {
          break;
        }
        boolean hasData = !isCellEmpty(firstDataCell);
        boolean hasDropdown = dropdownCellFactory.findColumnInRange(1, column)!=null;
        //columns need to be unlocked if they have a header and no data/dropdown
        if(!hasData && !hasDropdown) {
          unlockColumn(spreadsheet, column, 1);
        }
      }
    }

    private boolean isCellEmpty(Cell cell) {
      return cell==null || cell.getStringCellValue().isEmpty();
    }

    private void unlockColumn(Spreadsheet spreadsheet, int column, int minRow) {
      List<Cell> updatedCells = new ArrayList<>();
      CellStyle unLockedStyle = spreadsheet.getWorkbook().createCellStyle();
      unLockedStyle.setLocked(false);
      for (int i = minRow; i <= numberOfSamples; i++) {
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
        fixColumnWidth(spreadsheet, columnIndex, columnHeader, new ArrayList<>());
        Cell cell = spreadsheet.createCell(0, columnIndex, columnHeader);
        cell.setCellStyle(boldHeaderStyle);
        updatedCells.add(cell);
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

      DropDownColumn techColumn = new DropDownColumn().withItems(Arrays.stream(SequenceAnalysisTypes
              .values())
          .map(e -> e.label)
          .collect(Collectors.toList()));
      techColumn.fromRowIndex(1).toRowIndex(numberOfSamples).atColIndex(0);

      dropdownCellFactory.addDropdownColumn(techColumn);
      handleCommonMetadata(header, spreadsheet, dropdownCellFactory);
    }

    /**
     * Creates and adds species and specimen Dropdown columns to a spreadsheet via a provided
     * SpreadsheetDropdownFactory dropdown column objects are only created if more than one species
     * or specimen is part of this experiment. Otherwise, the value is added directly to the
     * respective cells.
     */
    private void handleCommonMetadata(List<String> header, Spreadsheet spreadsheet,
        SpreadsheetDropdownFactory dropdownCellFactory) {

      List<String> species = activeExperiment.getSpecies().stream().map(Species::label).toList();
      List<String> specimens = activeExperiment.getSpecimens().stream()
          .map(Specimen::label).toList();

      fixColumnWidth(spreadsheet, header.indexOf("Species"), "Species", species);
      fixColumnWidth(spreadsheet, header.indexOf("Specimen"), "Specimen", specimens);

      fillSampleSourceCells(header.indexOf("Species"), species, spreadsheet, dropdownCellFactory);
      fillSampleSourceCells(header.indexOf("Specimen"), specimens, spreadsheet, dropdownCellFactory);
      fillConditionCells(header.indexOf("Condition"), spreadsheet, dropdownCellFactory);
    }

    private void fillConditionCells(int colIndex, Spreadsheet spreadsheet,
        SpreadsheetDropdownFactory dropdownCellFactory) {
      List<ExperimentalGroup> groups = activeExperiment.getExperimentalGroups().stream().toList();
      List<String> conditionItems = new ArrayList<>();
      DropDownColumn variableDropdown = new DropDownColumn();
      // create condition items for dropdown and fix cell width
      for(ExperimentalGroup group : groups) {
        List<String> varStrings = new ArrayList<>();
        for(VariableLevel level : group.condition().getVariableLevels()) {
          String varName = level.variableName().value();
          String value = level.experimentalValue().value();
          String unit = "";
          if(level.experimentalValue().unit().isPresent()) {
            unit = " "+level.experimentalValue().unit().get();
          }
          varStrings.add(varName+":"+value+unit);
        }
        String conditionString = String.join("; ", varStrings);
        conditionItems.add(conditionString);
      }
      fixColumnWidth(spreadsheet, colIndex, "Condition", conditionItems);

      variableDropdown.toRowIndex(numberOfSamples).atColIndex(colIndex);
      variableDropdown.withItems(conditionItems);

      dropdownCellFactory.addDropdownColumn(variableDropdown);
    }

    /*
     * Changes width of a spreadsheet column based on header element and potential known entries.
     */
    private void fixColumnWidth(Spreadsheet spreadsheet, int colIndex, String header, List<String> entries) {
      final String COL_SPACER = "___";
      List<String> stringList = new ArrayList<>(Arrays.asList(header));
      stringList.addAll(entries);
      String longestString = stringList.stream().max(Comparator.comparingInt(String::length))
          .get();
      String spacingValue = longestString+COL_SPACER;
      Cell cell = spreadsheet.getCell(1, colIndex);
      String oldValue = "";
      if(cell==null) {
        spreadsheet.createCell(1, colIndex, spacingValue);
      } else {
        oldValue = cell.getStringCellValue();
        spreadsheet.getCell(1, colIndex).setCellValue(spacingValue);
      }
      spreadsheet.autofitColumn(colIndex);
      spreadsheet.getCell(1, colIndex).setCellValue(oldValue);
    }

    /**
     * Used to generate columns for Species and Specimen
     * @param colIndex
     * @param items
     * @param spreadsheet
     * @param dropdownCellFactory
     */
    private void fillSampleSourceCells(int colIndex, List<String> items, Spreadsheet spreadsheet,
        SpreadsheetDropdownFactory dropdownCellFactory) {

      List<Cell> updatedCells = new ArrayList<>();

      if(items.size() == 1) {
        for (int rowIndex = 1; rowIndex <= numberOfSamples; rowIndex++) {
          Cell cell = spreadsheet.createCell(rowIndex, colIndex, items.get(0));
          updatedCells.add(cell);
        }
      } else {
        DropDownColumn itemDropdown = new DropDownColumn();
        itemDropdown.withItems(items);
        itemDropdown.toRowIndex(numberOfSamples).atColIndex(colIndex);
        dropdownCellFactory.addDropdownColumn(itemDropdown);
      }
      spreadsheet.refreshCells(updatedCells);
    }

  }

  /**
   * SequenceAnalysisTypes enums are used in {@link SampleSpreadsheetLayout}, to indicate which type
   * of Analysis will be performed.
   *
   * @since 1.0.0
   */
  enum SequenceAnalysisTypes {
    RNASEQ("RNA-Seq"), DNASEQ("DNA-Seq");
    final String label;

    SequenceAnalysisTypes(String label) {
      this.label = label;
    }
  }

}
