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
import java.util.stream.StreamSupport;
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
  private List<String> analysisTypes;
  private List<String> species;
  private List<String> specimens;
  private List<String> analytes;

  //Spreadsheet component only allows retrieval of strings, so we have to store the experimentalGroupId separately
  private Map<String, ExperimentalGroup> experimentalGroupToConditionString;
  private Map<String, List<BiologicalReplicate>> conditionsToReplicates;
  private int numberOfSamples;

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
  public void setExperimentMetadata(Experiment experiment) {
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
    generateSheetDependentOnDataType(metaDataType);
    setRowColHeadingsVisible(false);
    setActiveSheetProtected("password-needed-to-lock");
    setSpreadsheetComponentFactory(dropdownCellFactory);
    //initialise first rows based on known sample size
    addRowsForInitialSamples(numberOfSamples);
    refreshAllCellValues();
    //Only reloads based on first row and first column with index = 1, meaning row and column style has to be refreshed manually
    reloadVisibleCellContents();
  }

  private void generateSheetDependentOnDataType(MetadataType metaDataType) {
    switch (metaDataType) {
      case PROTEOMICS -> generateProteomicsSheet();
      case LIGANDOMICS -> generateLigandomicsSheet();
      case TRANSCRIPTOMICS_GENOMICS -> generateGenomicsSheet();
      case METABOLOMICS -> generateMetabolomicsSheet();
    }
  }

  private void addRowsForInitialSamples(int numberOfSamples) {
    // + 1 header row
    setMaxRows(1);
    for (int currentRow = 1; currentRow <= numberOfSamples; currentRow++) {
      addRow();
    }
  }

  private void prepareConditionItems(List<ExperimentalGroup> groups) {
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
    ArrayList<Cell> updatedCells = new ArrayList<>();
    for (int columnIndex = 0; columnIndex < header.size(); columnIndex++) {
      SamplesheetHeaderName colHeader = header.get(columnIndex);
      switch (colHeader) {
        case ROW -> updatedCells.add(generateRowHeaderCell(columnIndex, increasedRowIndex));
        case SPECIES -> updatedCells.add(prefillCell(columnIndex, increasedRowIndex, species));
        case SEQ_ANALYSIS_TYPE ->
            updatedCells.add(prefillCell(columnIndex, increasedRowIndex, analysisTypes));
        case SAMPLE_LABEL, CUSTOMER_COMMENT ->
            updatedCells.add(prefillCell(columnIndex, increasedRowIndex, new ArrayList<>()));
        case BIOLOGICAL_REPLICATE_ID ->
            updatedCells.add(prefillCell(columnIndex, increasedRowIndex, getReplicateLabels()));
        case SPECIMEN -> updatedCells.add(prefillCell(columnIndex, increasedRowIndex, specimens));
        case ANALYTE -> updatedCells.add(prefillCell(columnIndex, increasedRowIndex, analytes));
        case CONDITION -> updatedCells.add(prefillCell(columnIndex, increasedRowIndex,
            conditionsToReplicates.keySet().stream().toList()));
      }
      styleRowCells(updatedCells);
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

  private Cell generateRowHeaderCell(int colIndex, int rowIndex) {
    CellStyle boldStyle = this.getWorkbook().createCellStyle();
    Font font = this.getWorkbook().createFont();
    font.setBold(true);
    boldStyle.setFont(font);
    boldStyle.setLocked(true);
    boldStyle.setAlignment(HorizontalAlignment.CENTER);
    Cell cell = this.createCell(rowIndex, colIndex, rowIndex);
    cell.setCellStyle(boldStyle);
    //ToDo this is a bottleneck which can impact performance but is necessary, because we allow users to add rows manually
    refreshCells(cell);
    return cell;
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
  private Cell prefillCell(int colIndex, int rowIndex, List<String> items) {
    Cell cell = this.createCell(rowIndex, colIndex, "");
    if (items.size() == 1) {
      cell.setCellValue(items.stream().findFirst().orElseThrow());
      CellStyle lockedStyle = this.getWorkbook().createCellStyle();
      lockedStyle.setLocked(true);
      cell.setCellStyle(lockedStyle);
    }
    return cell;
  }

  private void generateColumnsHeaders(LinkedHashMap<SamplesheetHeaderName,
      List<String>> cellValueOptionsMap) {
    List<Cell> headerCells = new ArrayList<>();
    setMaxColumns(cellValueOptionsMap.size());
    for (SamplesheetHeaderName columnHeader : cellValueOptionsMap.keySet()) {
      String columnLabel = columnHeader.label;
      int currentColumnIndex = columnHeader.ordinal();
      Cell cell = this.createCell(0, currentColumnIndex, columnLabel);
      headerCells.add(cell);
      List<String> cellValueOptions = cellValueOptionsMap.get(columnHeader);
      fixColumnWidth(currentColumnIndex, columnLabel,
          Objects.requireNonNullElseGet(cellValueOptions, ArrayList::new));
    }
    styleColumnHeaderCells(headerCells);
  }

  private void styleColumnHeaderCells(List<Cell> headerCells) {
    CellStyle boldHeaderStyle = this.getWorkbook().createCellStyle();
    Font font = this.getWorkbook().createFont();
    font.setBold(true);
    boldHeaderStyle.setFont(font);
    headerCells.forEach(cell -> cell.setCellStyle(boldHeaderStyle));
    refreshCells(headerCells);
  }

  private void styleRowCells(Collection<Cell> rowCells) {
    //cells need to be unlocked if they are not prefilled in any way
    defaultStyleAndUnlockEditableCells(rowCells);
  }

  private void defaultStyleAndUnlockEditableCells(Collection<Cell> rowCells) {
    CellStyle unLockedStyle = this.getWorkbook().createCellStyle();
    unLockedStyle.setWrapText(true);
    unLockedStyle.setLocked(false);
    rowCells.stream().filter(cell -> !isPrefilledColumn(cell.getColumnIndex()))
        .forEach(cell -> cell.setCellStyle(unLockedStyle));
  }

  /**
   * Changes width of a spreadsheet column based on header element and potential known entries.
   * Note: The autofit() column method does not account for components within the cell
   */
  private void fixColumnWidth(int colIndex, String colLabel,
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


  private void generateProteomicsSheet() {
    this.header = retrieveProteomics();
  }

  private void generateMetabolomicsSheet() {
    this.header = retrieveMetabolomics();
  }

  private void generateLigandomicsSheet() {
    this.header = retrieveLigandomics();
  }

  private void generateGenomicsSheet() {
    this.header = retrieveGenomics();
    LinkedHashMap<SamplesheetHeaderName, List<String>> cellValueOptionsMap = mapCellValueOptionsForColumns(
        header);
    generateColumnsHeaders(cellValueOptionsMap);
    dropdownCellFactory.setColumnValues(cellValueOptionsMap);
  }

  private LinkedHashMap<SamplesheetHeaderName, List<String>> mapCellValueOptionsForColumns(
      List<SamplesheetHeaderName> headerNames) {
    LinkedHashMap<SamplesheetHeaderName, List<String>> cellValueOptionsForColumnMap = new LinkedHashMap<>();
    analysisTypes = generateGenomicsAnalysisTypes();
    for (SamplesheetHeaderName head : headerNames) {
      cellValueOptionsForColumnMap.put(head, new ArrayList<>());
    }
    cellValueOptionsForColumnMap.put(SamplesheetHeaderName.SPECIES, species);
    cellValueOptionsForColumnMap.put(SamplesheetHeaderName.SPECIMEN, specimens);
    cellValueOptionsForColumnMap.put(SamplesheetHeaderName.ANALYTE, analytes);
    cellValueOptionsForColumnMap.put(SamplesheetHeaderName.CONDITION,
        conditionsToReplicates.keySet().stream().toList());
    cellValueOptionsForColumnMap.put(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE, analysisTypes);
    cellValueOptionsForColumnMap.put(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID,
        getReplicateLabels());
    return cellValueOptionsForColumnMap;
  }

  private List<String> generateGenomicsAnalysisTypes() {
    return Arrays.stream(SequenceAnalysisType
            .values())
        .map(e -> e.label)
        .collect(Collectors.toList());
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
    for (int rowId = 1; rowId < getRows(); rowId++) {
      Row row = getActiveSheet().getRow(rowId);
      // needed to highlight cells with missing values
      List<Integer> mandatoryInputCols = new ArrayList<>();
      for (SamplesheetHeaderName name : SamplesheetHeaderName.values()) {
        if (name.isMandatory) {
          mandatoryInputCols.add(header.indexOf(name));
        }
      }
      // Throw exception if null values in row.
      if (areNullCellsInRow(row)) {
        throw new IllegalArgumentException("null value provided in row" + row.getRowNum());
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
    }
    //We need to reset the style for cells with valid content if they were previously invalid
    defaultStyleAndUnlockEditableCells(validCells);
    if (!invalidCells.isEmpty()) {
      highlightInvalidCells(invalidCells);
      return Result.fromError(new InvalidSpreadsheetInput(
          SpreadsheetInvalidationReason.MISSING_INPUT));
    }
    return Result.fromValue(null);
  }

  private boolean areNullCellsInRow(Row row) {
    return StreamSupport.stream(row.spliterator(), false).anyMatch(Objects::isNull);
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

  public List<NGSRowDTO> getFilledRows() {
    List<NGSRowDTO> rows = new ArrayList<>();

    for (int rowId = 1; rowId < getRows(); rowId++) {
      Row row = getActiveSheet().getRow(rowId);

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
      if (areNullCellsInRow(row)) {
        throw new IllegalArgumentException("null value provided in row" + row.getRowNum());
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
