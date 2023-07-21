package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
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
  private final SpreadsheetDropdownFactory dropdownCellFactory = new SpreadsheetDropdownFactory();
  private List<SamplesheetHeaderName> header;
  private static List<String> species;
  private static List<String> specimens;
  private static List<String> analytes;

  //Spreadsheet component only allows retrieval of strings, so we have to store the experimentalGroupId separately
  private static Map<String, ExperimentalGroup> experimentalGroupToConditionString;
  private static Map<String, List<BiologicalReplicate>> conditionsToReplicates;
  private static int numberOfSamples;
  private transient Sheet sampleRegistrationSheet;

  public SampleRegistrationSpreadsheet() {
    this.addClassName("sample-spreadsheet");
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
    this.createNewSheet("SampleRegistrationSheet", 1, 1);
    this.deleteSheet(0);
    sampleRegistrationSheet = this.getActiveSheet();
    switch (metaDataType) {
      case PROTEOMICS -> addProteomicsSheet(retrieveProteomics());
      case LIGANDOMICS -> addLigandomicsSheet(retrieveLigandomics());
      case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(retrieveGenomics());
      case METABOLOMICS -> addMetabolomicsSheet(retrieveMetabolomics());
    }
    this.setRowColHeadingsVisible(false);
    this.setActiveSheetProtected("password-needed-to-lock");
    this.setSpreadsheetComponentFactory(dropdownCellFactory);
    //initialise first rows based on known sample size
    addRowsForInitialSamples(numberOfSamples);
  }

  private void addRowsForInitialSamples(int numberOfSamples) {
    // + 1 header row
    setMaxRows(1);
    for (int currentRow = 1; currentRow <= numberOfSamples; currentRow++) {
      addRow();
    }
    setMaxRows(numberOfSamples + 1);
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
    int lastRowIndex = getRows() - 1; // zero-based index
    int increasedRowIndex = lastRowIndex + 1;

    for (int columnIndex = 0; columnIndex < header.size(); columnIndex++) {
      SamplesheetHeaderName colHeader = header.get(columnIndex);
      switch (colHeader) {
        case SPECIES -> prefillCell(columnIndex, increasedRowIndex, species);
        case SPECIMEN -> prefillCell(columnIndex, increasedRowIndex, specimens);
        case ANALYTE -> prefillCell(columnIndex, increasedRowIndex, analytes);
        case CONDITION -> prefillCell(columnIndex, increasedRowIndex,
            conditionsToReplicates.keySet().stream().toList());
        case BIOLOGICAL_REPLICATE_ID ->
            prefillCell(columnIndex, increasedRowIndex, getReplicateLabels());
        case ROW -> fillEnumerationCell(columnIndex, increasedRowIndex);
      }
      //cells need to be unlocked if they are not prefilled in any way
      enableEditableCellsOfColumnUntilRow(increasedRowIndex, columnIndex);
    }
    setMaxRows(increasedRowIndex + 1); // 1-based
  }

  private boolean isPrefilledColumn(int columnIndex) {
    SamplesheetHeaderName colHeader = header.get(columnIndex);
    return switch (colHeader) {
      case ROW, SEQ_ANALYSIS_TYPE, BIOLOGICAL_REPLICATE_ID, SPECIES, SPECIMEN, ANALYTE, CONDITION ->
          true;
      default -> false;
    };
  }

  private void fillEnumerationCell(int colIndex, int rowIndex) {
    CellStyle boldStyle = this.getWorkbook().createCellStyle();
    Font font = this.getWorkbook().createFont();
    font.setBold(true);
    boldStyle.setFont(font);
    boldStyle.setLocked(true);
    boldStyle.setAlignment(HorizontalAlignment.CENTER);
    Cell cell = this.createCell(rowIndex, colIndex, rowIndex);
    cell.setCellStyle(boldStyle);
    this.refreshCells(cell);
  }

  /**
   * Delete a row, remove it from visual representation by shifting following rows up.
   *
   * @param index 0-based row index of the row to remove
   */
  public void deleteRow(int index) {
    if (getRows() == 1) {
      // only one row remaining -> the header row
      return;
    }
    //delete row
    deleteRows(index, index);
    //move other rows up
    if (index + 1 < getRows()) {
      shiftRows(index + 1, getRows() - 1, -1, true, true);
    }
    setMaxRows(getRows() - 1);
  }


  /**
   * Generates and prefills the correct cell components dependent on already specified values.
   *
   * @param colIndex
   * @param rowIndex
   * @param items
   */
  private void prefillCell(int colIndex, int rowIndex, List<String> items) {
    if (items.size() == 1) {
      Cell cell = this.createCell(rowIndex, colIndex, items.get(0));
      CellStyle lockedStyle = this.getWorkbook().createCellStyle();
      lockedStyle.setLocked(true);
      cell.setCellStyle(lockedStyle);
      this.refreshCells(cell);
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
    this.reloadVisibleCellContents();
  }

  void enableEditableCellsOfColumnUntilRow(int maxRow, int column) {
    Set<Cell> cells = new HashSet<>();
    for (int row = 1; row <= maxRow; row++) {
      if (isCellUnused(this.getCell(row, column))) {
        Cell cell = this.createCell(row, column, "");
        cells.add(cell);
      }
    }
    defaultStyleAndUnlockEditableCells(cells);
  }

  void defaultStyleAndUnlockEditableCells(Set<Cell> cells) {
    CellStyle unLockedStyle = this.getWorkbook().createCellStyle();
    unLockedStyle.setLocked(false);
    for (Cell cell : cells) {
      if (!isPrefilledColumn(cell.getColumnIndex())) {
        cell.setCellStyle(unLockedStyle);
      }
    }
    this.refreshCells(cells);
  }

  //an unused cell is either null or empty (not blank)
  private boolean isCellUnused(Cell cell) {
    return cell == null || SpreadsheetMethods.cellToStringOrNull(cell).isEmpty();
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
        .orElseThrow();
    String spacingValue = longestString + COL_SPACER;
    Cell cell = this.getCell(0, colIndex);
    String oldValue = "";
    if (cell == null) {
      this.createCell(0, colIndex, spacingValue);
    } else {
      oldValue = SpreadsheetMethods.cellToStringOrNull(cell);
      this.getCell(0, colIndex).setCellValue(spacingValue);
    }
    //Todo Find out why switching from a sheet with less columns to a sheet with more columns breaks the sheet(e.g. lipidomics to genomics)
    try {
      this.autofitColumn(colIndex);
      this.getActiveSheet();
    } catch (IndexOutOfBoundsException exception) {
      throw new RuntimeException("Something went wrong when switching sheets.");
    }

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
    setDefaultColumnCount(header.size());
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
    HashMap<Integer, List<String>> columnValues = new HashMap<>();
    headerToPresets.forEach(
        (samplesheetHeaderName, strings) -> columnValues.put(samplesheetHeaderName.ordinal(),
            strings));
    dropdownCellFactory.setColumnValues(columnValues);
    prepareColumnHeaderAndWidth(headerToPresets);
    this.reloadVisibleCellContents();
  }


  /**
   * The SamplesheetHeaderName enum contains the labels which are used to refer to the headers
   * employed during sample batch registration for different data technologies
   *
   * @since 1.0.0
   */
  public enum SamplesheetHeaderName {
    ROW("#", false), SEQ_ANALYSIS_TYPE("Analysis to be performed",
        true), SAMPLE_LABEL("Sample label", true),
    BIOLOGICAL_REPLICATE_ID("Biological replicate id", true),
    CONDITION("Condition", true), SPECIES("Species", true),
    SPECIMEN("Specimen", true), ANALYTE("Analyte", true),
    CUSTOMER_COMMENT("Customer comment", false);

    public final String label;
    public final boolean isMandatory;

    SamplesheetHeaderName(String label, boolean isMandatory) {
      this.label = label;
      this.isMandatory = isMandatory;
    }
  }

  public List<SamplesheetHeaderName> retrieveProteomics() {
    return List.of(SamplesheetHeaderName.ROW, SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveLigandomics() {
    return List.of(SamplesheetHeaderName.ROW, SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveMetabolomics() {
    return List.of(SamplesheetHeaderName.ROW, SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveGenomics() {
    return List.of(SamplesheetHeaderName.ROW, SamplesheetHeaderName.SEQ_ANALYSIS_TYPE,
        SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public Result<Void, InvalidSpreadsheetInput> areInputsValid() {
    Set<Cell> invalidCells = new HashSet<>();
    Set<Cell> validCells = new HashSet<>();
    for (int rowId = 1; rowId <= sampleRegistrationSheet.getLastRowNum(); rowId++) {
      Row row = sampleRegistrationSheet.getRow(rowId);
      // needed to highlight cells with missing values
      List<Integer> mandatoryInputCols = new ArrayList<>();
      // needed to find which cells have missing values
      List<String> mandatoryInputs = new ArrayList<>();
      for (SamplesheetHeaderName name : SamplesheetHeaderName.values()) {
        if (name.isMandatory) {
          mandatoryInputs.add(SpreadsheetMethods.cellToStringOrNull(row.getCell(
              header.indexOf(name))));
          mandatoryInputCols.add(header.indexOf(name));
        }
      }
      // break when cells in row are undefined
      if (mandatoryInputs.stream().anyMatch(Objects::isNull)) {
        break;
      }

      // mandatory not filled in --> invalid
      for (int colId : mandatoryInputCols) {
        Cell cell = row.getCell(colId);
        if (SpreadsheetMethods.cellToStringOrNull(cell).isBlank()) {
          invalidCells.add(cell);
        } else {
          validCells.add(cell);
        }
      }

      String replicateIDInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
          header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID))).trim();
    }
    defaultStyleAndUnlockEditableCells(validCells);
    if (!invalidCells.isEmpty()) {
      highlightInvalidCells(invalidCells);

      return Result.fromError(new InvalidSpreadsheetInput(
          SpreadsheetInvalidationReason.MISSING_INPUT));
    }
    return Result.fromValue(null);
  }

  private void highlightInvalidCells(Collection<Cell> cells) {
    CellStyle invalidStyle = this.getWorkbook().createCellStyle();
    invalidStyle.setLocked(false);
    invalidStyle.setBorderTop(BorderStyle.THIN);
    invalidStyle.setBorderLeft(BorderStyle.THIN);
    invalidStyle.setBorderRight(BorderStyle.THIN);
    invalidStyle.setBorderBottom(BorderStyle.THIN);

    short redIndex = IndexedColors.RED.getIndex();
    invalidStyle.setBottomBorderColor(redIndex);
    invalidStyle.setTopBorderColor(redIndex);
    invalidStyle.setLeftBorderColor(redIndex);
    invalidStyle.setRightBorderColor(redIndex);

    for (Cell cell : cells) {
      cell.setCellStyle(invalidStyle);
    }
    this.refreshCells(cells);
  }

  private boolean isUniqueSampleRow(Collection<String> knownIDs, Row row) {
    String replicateIDInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
        header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID))).trim();
    String conditionInput = SpreadsheetMethods.cellToStringOrNull(row.getCell(
        header.indexOf(SamplesheetHeaderName.CONDITION))).trim();
    // Sample uniqueness needs to be guaranteed by condition and replicate ID
    String concatenatedSampleID = replicateIDInput + conditionInput;
    if (knownIDs.contains(concatenatedSampleID)) {
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
              replicateIDInput, conditionInput, speciesInput, specimenInput, analyteInput)
          .anyMatch(Objects::isNull)) {
        break;
      }

      String conditionString = conditionInput.trim();
      String replicateIDString = replicateIDInput.trim();

      ExperimentalGroup experimentalGroup = experimentalGroupToConditionString.get(conditionString);
      Long experimentalGroupId = experimentalGroup.id();
      BiologicalReplicateId biologicalReplicateId = retrieveBiologicalReplicateId(replicateIDString,
          conditionString);
      rows.add(
          new NGSRowDTO(analysisTypeInput.trim(), sampleLabelInput.trim(), biologicalReplicateId,
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

  public static class InvalidSpreadsheetInput {

    private final int invalidRow;
    private final SpreadsheetInvalidationReason reason;
    private final String additionalInfo;

    InvalidSpreadsheetInput(SpreadsheetInvalidationReason reason, int invalidRow,
        String additionalInfo) {
      this.reason = reason;
      this.invalidRow = invalidRow;
      this.additionalInfo = additionalInfo;
    }

    InvalidSpreadsheetInput(SpreadsheetInvalidationReason reason) {
      this(reason, 0, "");
    }

    InvalidSpreadsheetInput(SpreadsheetInvalidationReason reason, int invalidRow) {
      this(reason, invalidRow, "");
    }

    /**
     * Returns a String mentioning the invalid row of the spreadsheet and the reason why it is
     * invalid. If this object was created with additional information on the reason, it is added.
     *
     * @return String stating row and reason for the row being invalid
     */
    public String getInvalidationReason() {
      String message = switch (reason) {
        case MISSING_INPUT:
          yield "Please complete the missing mandatory information.";
        case DUPLICATE_ID:
          yield "Biological replicate Id was used multiple times for the "
              + "same condition in row " + invalidRow + ".";
      };
      if (!additionalInfo.isEmpty()) {
        message += ": " + additionalInfo;
      }

      return message;
    }
  }

  enum SpreadsheetInvalidationReason {
    MISSING_INPUT, DUPLICATE_ID
  }

}
