package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class SampleRegistrationSpreadsheet extends Spreadsheet implements Serializable {

  @Serial
  private static final long serialVersionUID = 573778360298068552L;

  private SpreadsheetDropdownFactory dropdownCellFactory;
  private List<SamplesheetHeaderName> header;
  private static List<String> species;
  private static List<String> specimens;
  private static List<String> analytes;

  //Spreadsheet component only allows retrieval of strings so we have to store the experimentalGroupId separately
  private static Map<String, ExperimentalGroup> experimentalGroupToConditionString;
  private static Map<String, List<BiologicalReplicate>> conditionsToReplicates;
  private static int numberOfSamples;
  private transient Sheet sampleRegistrationSheet;

  public SampleRegistrationSpreadsheet() {
    // The SampleRegistrationSpreadsheet component only makes sense once information has been filled via the experiment information,
    // which can only happen once the experiment is loaded, therefore the setExperimentMetadata() method should be used.
  }

  /**
   * Sets an experiment in order to provide the spreadsheet builder with known metadata to prefill
   * certain columns
   *
   * @param experiment An Experiment object, most likely the active one
   */
  public static void setExperimentMetadata(Experiment experiment) {
    species = experiment.getSpecies().stream().map(Species::label).toList();
    specimens = experiment.getSpecimens().stream()
        .map(Specimen::label).toList();
    analytes = experiment.getAnalytes().stream()
        .map(Analyte::label).toList();
    List<ExperimentalGroup> groups = experiment.getExperimentalGroups().stream().toList();
    numberOfSamples = groups.stream().map(ExperimentalGroup::sampleSize)
        .mapToInt(Integer::intValue).sum();
    prepareConditionItems(groups);
  }

  public void addSheetToSpreadsheet(MetadataType metaDataType) {

    dropdownCellFactory = new SpreadsheetDropdownFactory();
    this.createNewSheet("SampleRegistrationSheet", 1, 1);
    this.deleteSheet(0);
    sampleRegistrationSheet = this.getActiveSheet();
    switch (metaDataType) {
      case PROTEOMICS -> addProteomicsSheet(retrieveProteomics());
      case LIGANDOMICS -> addLigandomicsSheet(retrieveLigandomics());
      case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(retrieveGenomics());
      case METABOLOMICS -> addMetabolomicsSheet(retrieveMetabolomics());
    }
    this.setActiveSheetProtected("password-needed-to-lock");
    this.setSpreadsheetComponentFactory(dropdownCellFactory);
    //initialise first rows based on known sample size
    addRowsForInitialSamples(numberOfSamples);
  }

  private void addRowsForInitialSamples(int numberOfSamples) {
    for (int currentRow = 1; currentRow <= numberOfSamples; currentRow++) {
      addRow();
    }
  }

  private static void prepareConditionItems(List<ExperimentalGroup> groups) {
    // create condition items for dropdown and fix cell width. Remember replicates for each condition
    conditionsToReplicates = new HashMap<>();
    experimentalGroupToConditionString = new HashMap<>();
    for (ExperimentalGroup group : groups) {
      List<String> varStrings = new ArrayList<>();
      for (VariableLevel level : group.condition().getVariableLevels()) {
        String varName = level.variableName().value();
        String value = level.experimentalValue().value();
        String unit = "";
        if (level.experimentalValue().unit().isPresent()) {
          unit = " " + level.experimentalValue().unit().get();
        }
        varStrings.add(varName + ":" + value + unit);
      }
      String conditionString = String.join("; ", varStrings);
      conditionsToReplicates.put(conditionString.trim(), group.biologicalReplicates());
      experimentalGroupToConditionString.put(conditionString.trim(), group);
    }
  }

  private List<String> getReplicateLabels() {
    Set<String> replicateLabels = new TreeSet<>();
    for (List<BiologicalReplicate> replicates : conditionsToReplicates.values()) {
      replicateLabels.addAll(replicates.stream().map(BiologicalReplicate::label).toList());
    }
    return replicateLabels.stream().toList();
  }

  /**
   * Adds rows to the spreadsheet that contains prefilled data, selectable dropdowns and editable
   * free-text cells. The rows are added below the last row containing data.
   */
  public void addRow() {
    int lastRowIndex = 1 + sampleRegistrationSheet.getLastRowNum();
    for (int columnIndex = 0; columnIndex < header.size(); columnIndex++) {
      SamplesheetHeaderName colHeader = header.get(columnIndex);
      switch (colHeader) {
        case SPECIES -> prefillCellsToRow(columnIndex, lastRowIndex, species);
        case SPECIMEN -> prefillCellsToRow(columnIndex, lastRowIndex, specimens);
        case ANALYTE -> prefillCellsToRow(columnIndex, lastRowIndex, analytes);
        case CONDITION -> prefillCellsToRow(columnIndex, lastRowIndex,
            conditionsToReplicates.keySet().stream().toList());
        case BIOLOGICAL_REPLICATE_ID ->
            prefillCellsToRow(columnIndex, lastRowIndex, getReplicateLabels());
        default -> {
          DropdownColumn column = dropdownCellFactory.getColumn(columnIndex);
          if (column != null) {
            column.increaseToRow(lastRowIndex);
          }
        }
      }

      Cell newCell = this.getCell(lastRowIndex, columnIndex);
      boolean hasData = !this.isCellUnused(newCell);
      boolean hasDropdown = dropdownCellFactory.findColumnInRange(1, columnIndex) != null;
      //cells need to be unlocked if they have no data/dropdown
      if (!hasData && !hasDropdown) {
        this.unlockCellsToRow(this, lastRowIndex, columnIndex);
      }
    }
  }


  /**
   * Generates and prefills the correct cell components dependent on already specified values.
   *
   * @param colIndex
   * @param rowIndex
   * @param items
   */
  private void prefillCellsToRow(int colIndex, int rowIndex, List<String> items) {
    if (items.size() == 1) {
      Cell cell = this.createCell(rowIndex, colIndex, items.get(0));
      this.refreshCells(cell);
    } else {
      dropdownCellFactory.addDropDownCell(rowIndex, colIndex);
    }
  }

  private void setupCommonDropDownColumns() {
    initDropDownColumn(header.indexOf(SamplesheetHeaderName.SPECIES), species);
    initDropDownColumn(header.indexOf(SamplesheetHeaderName.SPECIMEN), specimens);
    initDropDownColumn(header.indexOf(SamplesheetHeaderName.ANALYTE), analytes);
    initDropDownColumn(header.indexOf(SamplesheetHeaderName.CONDITION),
        conditionsToReplicates.keySet().stream().toList());
    initDropDownColumn(header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID),
        getReplicateLabels());
  }

  private void initDropDownColumn(int colIndex, List<String> items) {
    if (items.size() > 1) {
      DropdownColumn itemDropDown = new DropdownColumn();
      itemDropDown.withItems(items);
      itemDropDown.toRowIndex(0).atColIndex(colIndex);
      dropdownCellFactory.addDropdownColumn(itemDropDown);
    }
  }

  private void prepareColumnHeaderAndWidth(LinkedHashMap<SamplesheetHeaderName,
      List<String>> headerToPresets) {
    CellStyle boldHeaderStyle = this.getWorkbook().createCellStyle();
    Font font = this.getWorkbook().createFont();
    font.setBold(true);
    boldHeaderStyle.setFont(font);
    List<Cell> updatedCells = new ArrayList<>();
    int columnIndex = 0;
    for (SamplesheetHeaderName columnHeader : headerToPresets.keySet()) {
      List<String> presets = headerToPresets.get(columnHeader);
      String columnLabel = columnHeader.label;
      this.fixColumnWidth(columnIndex, columnLabel,
          Objects.requireNonNullElseGet(presets, ArrayList::new));
      Cell cell = this.createCell(0, columnIndex, columnLabel);
      cell.setCellStyle(boldHeaderStyle);
      updatedCells.add(cell);
      columnIndex++;
    }
    this.refreshCells(updatedCells);
  }

  private void prepareCommonSheetTasks() {
    LinkedHashMap<SamplesheetHeaderName, List<String>> headerToPresets = new LinkedHashMap<>();
    for (SamplesheetHeaderName label : header) {
      headerToPresets.put(label, new ArrayList<>());
    }
    headerToPresets.put(SamplesheetHeaderName.SPECIES, species);
    headerToPresets.put(SamplesheetHeaderName.SPECIMEN, specimens);
    headerToPresets.put(SamplesheetHeaderName.ANALYTE, analytes);
    headerToPresets.put(SamplesheetHeaderName.CONDITION,
        conditionsToReplicates.keySet().stream().toList());
    headerToPresets.put(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, getReplicateLabels());
    prepareColumnHeaderAndWidth(headerToPresets);
    this.reload();
    setupCommonDropDownColumns();
  }


  public int findLastRow() {
    int res = 0;
    for (int row = 1; row <= Integer.MAX_VALUE; row++) {
      List<Cell> thisRow = new ArrayList<>();
      for (int col = 0; col <= header.size(); col++) {
        thisRow.add(this.getCell(row, col));
      }
      if (thisRow.stream().allMatch(Objects::isNull)) {
        break;
      }
      res = row;
    }
    return res;
  }

  void unlockCellsToRow(Spreadsheet spreadsheet, int maxRow, int column) {
    List<Cell> cells = new ArrayList<>();
    CellStyle unLockedStyle = spreadsheet.getWorkbook().createCellStyle();
    unLockedStyle.setLocked(false);
    for (int row = 1; row <= maxRow; row++) {
      if (isCellUnused(spreadsheet.getCell(row, column))) {
        Cell cell = spreadsheet.createCell(row, column, "");
        cell.setCellStyle(unLockedStyle);
        cells.add(cell);
      }
    }
    spreadsheet.refreshCells(cells);
  }

  //an unused cell is either null or empty (not blank)
  private boolean isCellUnused(Cell cell) {
    return cell == null || cell.getStringCellValue().isEmpty();
  }


  /*
   * Changes width of a spreadsheet column based on header element and potential known entries.
   */
  void fixColumnWidth(int colIndex, String colLabel,
      List<String> entries) {
    final String COL_SPACER = "___";
    List<String> stringList = new ArrayList<>(Collections.singletonList(colLabel));
    stringList.addAll(entries);
    String longestString = stringList.stream().max(Comparator.comparingInt(String::length))
        .get();
    String spacingValue = longestString + COL_SPACER;
    Cell cell = this.getCell(0, colIndex);
    String oldValue = "";
    if (cell == null) {
      this.createCell(0, colIndex, spacingValue);
    } else {
      oldValue = cell.getStringCellValue();
      this.getCell(0, colIndex).setCellValue(spacingValue);
    }
    this.autofitColumn(colIndex);
    this.getCell(0, colIndex).setCellValue(oldValue);
  }


  private void addProteomicsSheet(List<SamplesheetHeaderName> header) {
    this.header = header;
    prepareCommonSheetTasks();
    setDefaultColumnCount(header.size());
  }

  private void addMetabolomicsSheet(List<SamplesheetHeaderName> header) {
    this.header = header;
    prepareCommonSheetTasks();
    setDefaultColumnCount(header.size());
  }

  private void addLigandomicsSheet(List<SamplesheetHeaderName> header) {
    this.header = header;
    prepareCommonSheetTasks();
    setDefaultColumnCount(header.size());
  }

  private void addGenomicsSheet(List<SamplesheetHeaderName> header) {
    this.header = header;
    LinkedHashMap<SamplesheetHeaderName, List<String>> headerToPresets = new LinkedHashMap<>();
    for (SamplesheetHeaderName head : header) {
      headerToPresets.put(head, new ArrayList<>());
    }
    headerToPresets.put(SamplesheetHeaderName.SPECIES, species);
    headerToPresets.put(SamplesheetHeaderName.SPECIMEN, specimens);
    headerToPresets.put(SamplesheetHeaderName.ANALYTE, analytes);
    headerToPresets.put(SamplesheetHeaderName.CONDITION,
        conditionsToReplicates.keySet().stream().toList());
    headerToPresets.put(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE,
        Arrays.stream(SequenceAnalysisType
                .values())
            .map(e -> e.label)
            .collect(Collectors.toList()));
    headerToPresets.put(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, getReplicateLabels());
    prepareColumnHeaderAndWidth(headerToPresets);
    this.reload();
    DropdownColumn analysisTypeColumn = new DropdownColumn().withItems(
        Arrays.stream(SequenceAnalysisType
                .values())
            .map(e -> e.label)
            .collect(Collectors.toList()));
    analysisTypeColumn.toRowIndex(0)
        .atColIndex(header.indexOf(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE));

    dropdownCellFactory.addDropdownColumn(analysisTypeColumn);
    setupCommonDropDownColumns();
    setDefaultColumnCount(header.size());
  }


  /**
   * The SamplesheetHeaderName enum contains the labels which are used to refer to the headers
   * employed during sample batch registration for different data technologies
   *
   * @since 1.0.0
   */
  public enum SamplesheetHeaderName {
    SEQ_ANALYSIS_TYPE("Analysis to be performed", true), SAMPLE_LABEL(
        "Sample label", true), BIOLOGICAL_REPLICATE_ID(
            "Biological replicate id", true), CONDITION("Condition",
        true), SPECIES("Species", true), SPECIMEN("Specimen",
        true), ANALYTE("Analyte", true), CUSTOMER_COMMENT(
        "Customer comment", false);

    public final String label;
    public final boolean isMandatory;

    SamplesheetHeaderName(String label, boolean isMandatory) {
      this.label = label;
      this.isMandatory = isMandatory;
    }
  }

  public List<SamplesheetHeaderName> retrieveProteomics() {
    return List.of(SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveLigandomics() {
    return List.of(SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveMetabolomics() {
    return List.of(SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveGenomics() {
    return List.of(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE, SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public Result<Void, InvalidSpreadsheetRow> areInputsValid() {
    Set<String> concatenatedSampleIDs = new HashSet<>();
    for (int rowId = 2; rowId <= sampleRegistrationSheet.getLastRowNum(); rowId++) {
      Row row = sampleRegistrationSheet.getRow(rowId);
      List<String> mandatoryInputs = new ArrayList<>();
      for (SamplesheetHeaderName name : SamplesheetHeaderName.values()) {
        if (name.isMandatory) {
          mandatoryInputs.add(SpreadsheetMethods.cellToStringOrNull(row.getCell(
              header.indexOf(name))));
        }
      }
      // break when cells in row are undefined
      if (mandatoryInputs.stream().anyMatch(Objects::isNull)) {
        break;
      }

      // mandatory not filled in --> invalid
      if (mandatoryInputs.stream().anyMatch(String::isBlank)) {
        return Result.fromError(new InvalidSpreadsheetRow(
            SpreadsheetInvalidationReason.MISSING_INPUT, rowId));
      }

      String replicateIDInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID))).trim();
      // Sample uniqueness needs to be guaranteed by condition and replicate ID
      if (!isUniqueSampleRow(concatenatedSampleIDs, row)) {
        return Result.fromError(new InvalidSpreadsheetRow(
            SpreadsheetInvalidationReason.DUPLICATE_ID, rowId, replicateIDInput));
      }
    }
    return Result.fromValue(null);
  }

  private boolean isUniqueSampleRow(Set<String> knownIDs, Row row) {
    String replicateIDInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
        header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID))).trim();
    String conditionInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
        header.indexOf(SamplesheetHeaderName.CONDITION))).trim();
    // Sample uniqueness needs to be guaranteed by condition and replicate ID
    String concatenatedSampleID = replicateIDInput + conditionInput;
    if(knownIDs.contains(concatenatedSampleID)) {
      return false;
    } else {
      knownIDs.add(concatenatedSampleID);
      return true;
    }
  }

  public List<NGSRowDTO> getFilledRows() {
    List<NGSRowDTO> rows = new ArrayList<>();

    for (int rowId = 1; rowId <= sampleRegistrationSheet.getLastRowNum(); rowId++) {
      Row row = sampleRegistrationSheet.getRow(rowId);

      String analysisTypeInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE)));
      String sampleLabelInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.SAMPLE_LABEL)));
      String replicateIDInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID)));
      String conditionInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.CONDITION)));
      String speciesInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.SPECIES)));
      String specimenInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.SPECIMEN)));
      String analyteInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.ANALYTE)));
      String commentInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.CUSTOMER_COMMENT)));

      // break when cells in row are undefined
      if (Stream.of(analysisTypeInput, sampleLabelInput,
          replicateIDInput, conditionInput, speciesInput, specimenInput, analyteInput).anyMatch(Objects::isNull)) {
        break;
      }

      String conditionString = conditionInput.trim();
      String replicateIDString = replicateIDInput.trim();

      ExperimentalGroup experimentalGroup = experimentalGroupToConditionString.get(conditionString);
      Long experimentalGroupId = experimentalGroup.id();
      BiologicalReplicateId biologicalReplicateId = retrieveBiologicalReplicateId(replicateIDString,
          conditionString);
      rows.add(new NGSRowDTO(analysisTypeInput.trim(), sampleLabelInput.trim(), biologicalReplicateId,
          experimentalGroupId, speciesInput.trim(), specimenInput.trim(), analyteInput.trim(),
          commentInput.trim()));
    }
    return rows;
  }

  private BiologicalReplicateId retrieveBiologicalReplicateId(String replicateLabel,
      String condition) {
    Optional<BiologicalReplicate> biologicalReplicate = conditionsToReplicates.get(condition)
        .stream()
        .filter(bioRep -> bioRep.label().equals(replicateLabel)).findFirst();
    BiologicalReplicateId biologicalReplicateId;
    if (biologicalReplicate.isPresent()) {
      biologicalReplicateId = biologicalReplicate.get().id();
    } else {
      biologicalReplicateId = BiologicalReplicateId.create();
    }
    return biologicalReplicateId;
  }

  public record NGSRowDTO(String analysisType, String sampleLabel,
                          BiologicalReplicateId bioReplicateID,
                          Long experimentalGroupId, String species, String specimen, String analyte,
                          String customerComment) {

  }

  /**
   * SequenceAnalysisType enums are used in {@link SampleSpreadsheetLayout}, to indicate which type
   * of Analysis will be performed.
   *
   * @since 1.0.0
   */
  enum SequenceAnalysisType {
    RNASEQ("RNA-Seq"), DNASEQ("DNA-Seq");
    final String label;

    SequenceAnalysisType(String label) {
      this.label = label;
    }
  }

  public static class InvalidSpreadsheetRow {

    private final int invalidRow;
    private final SpreadsheetInvalidationReason reason;
    private final String additionalInfo;

    InvalidSpreadsheetRow(SpreadsheetInvalidationReason reason, int invalidRow, String additionalInfo) {
      this.reason = reason;
      this.invalidRow = invalidRow;
      this.additionalInfo = additionalInfo;
    }

    InvalidSpreadsheetRow(SpreadsheetInvalidationReason reason, int invalidRow) {
      this(reason, invalidRow, "");
    }

    /**
     * Returns a String mentioning the invalid row of the spreadsheet and the reason
     * why it is invalid. If this object was created with additional information on
     * the reason, it is added.
     *
     * @return String stating row and reason for the row being invalid
     */
    public String getInvalidationReason() {
      String message = switch (reason) {
        case MISSING_INPUT: yield "Mandatory information missing in row "+ invalidRow;
        case DUPLICATE_ID: yield "Biological replicate Id was used multiple times for the "
              + "same condition in row "+invalidRow;
      };
      if(!additionalInfo.isEmpty()) {
        message += ": "+additionalInfo;
      }

      return message;
    }
  }

  enum SpreadsheetInvalidationReason {
    MISSING_INPUT, DUPLICATE_ID
  }

}
