package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import java.awt.Color;
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
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicate.LexicographicLabelComparator;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId;
import life.qbic.projectmanagement.domain.project.experiment.Condition;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.VariableName;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.sample.AnalysisMethod;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ExtendedColor;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;

/**
 * The SampleRegistrationSpreadSheet is a {@link Spreadsheet} based component which enables the
 * registration of {@link MetadataType} specific Sample information.
 * <p>
 * The spreadsheet enables the user to provide and change the sample information of Samples for the
 * provided {@link Experiment}
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
  LinkedHashMap<SamplesheetHeaderName, List<String>> cellValueOptionsForColumnMap = new LinkedHashMap<>();
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

  /**
   * Fills in the default active sheet of the spreadsheet component dependent on the provided
   * {@link MetadataType}
   *
   * @param metaDataType the data type dependent on the chosen facility for which a sheet should be
   *                     generated
   */
  public void addSheetToSpreadsheet(MetadataType metaDataType) {
    generateSheetDependentOnDataType(metaDataType);
    setRowColHeadingsVisible(false);
    setActiveSheetProtected("password-needed-to-lock");
    setSpreadsheetComponentFactory(dropdownCellFactory);
    //initialise first rows based on known sample size
    addRowsForInitialSamples(numberOfSamples);
  }

  public void reloadSpreadsheet() {
    refreshAllCellValues();
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

  /**
   * Generates and adds Rows for the aggregated number of {@link BiologicalReplicate} within the
   * {@link ExperimentalGroup} of an Experiment
   *
   * @param numberOfSamples the count of already defined samples within the setExperiment of this
   *                        spreadsheet
   */
  private void addRowsForInitialSamples(int numberOfSamples) {
    // + 1 header row
    setMaxRows(1);
    for (int currentRow = 1; currentRow <= numberOfSamples; currentRow++) {
      addRow();
    }
  }

  /**
   * Extracts and aggregates the String values into the selectable CellValueOptions within the
   * ConditionColumn of all {@link VariableName} and {@link VariableLevel} for each
   * {@link Condition} of each {@link ExperimentalGroup} within the provided experiment
   *
   * @param groups List of all experimental groups defined within the set Experiment
   */
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

  /**
   * Extracts and aggregates the String based label values to the selectable CellValueOptions within
   * the ConditionColumn for all {@link BiologicalReplicate} within the {@link ExperimentalGroup} of
   * the provided experiment
   */
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
        case ROW -> generateRowHeaderCell(columnIndex, increasedRowIndex);
        case SPECIES -> generatePrefilledCell(columnIndex, increasedRowIndex, species);
        case SEQ_ANALYSIS_TYPE ->
            generatePrefilledCell(columnIndex, increasedRowIndex, analysisTypes);
        case SAMPLE_LABEL, CUSTOMER_COMMENT ->
            generatePrefilledCell(columnIndex, increasedRowIndex, new ArrayList<>());
        case BIOLOGICAL_REPLICATE_ID ->
            generatePrefilledCell(columnIndex, increasedRowIndex, getReplicateLabels());
        case SPECIMEN -> generatePrefilledCell(columnIndex, increasedRowIndex, specimens);
        case ANALYTE -> generatePrefilledCell(columnIndex, increasedRowIndex, analytes);
        case CONDITION -> generatePrefilledCell(columnIndex, increasedRowIndex,
            conditionsToReplicates.keySet().stream().toList());
      }
    }
    setMaxRows(increasedRowIndex + 1); // 1-based
  }

  /**
   * Generates and fills the cells in the first column with the current RowIndex and Header specific
   * style functioning so the column functions a row header
   */
  private void generateRowHeaderCell(int colIndex, int rowIndex) {
    CellStyle boldStyle = this.getWorkbook().createCellStyle();
    Font font = this.getWorkbook().createFont();
    font.setBold(true);
    boldStyle.setFont(font);
    boldStyle.setLocked(true);
    boldStyle.setAlignment(HorizontalAlignment.CENTER);
    Cell cell = this.createCell(rowIndex, colIndex, rowIndex);
    cell.setCellStyle(boldStyle);
    //This is a bottleneck which can impact performance but is necessary, because we allow users to add rows manually
    refreshCells(cell);
  }

  /**
   * Delete a row, remove it from visual representation by shifting following rows up.
   *
   * @param index 0-based row index of the row to remove
   */
  public void deleteRow(int index) {
    //delete row
    if (getRows() == 1) {
      // only one row remaining -> the header row
      return;
    }
    deleteRows(index, index);
    //move other rows up
    if (index + 1 < getRows()) {
      shiftRows(index + 1, getRows() - 1, -1, true, true);
    }
    setMaxRows(getRows() - 1);
  }


  /**
   * Generates a new cell with a prefilled value and style if only one value was provided, otherwise
   * the cell is filled with an empty string.
   *
   * @param colIndex columnIndex of the cell to be generated and prefilled
   * @param rowIndex rowIndex of the cell to be generated and prefilled
   * @param items    list of items which should be selectable within the cell
   */
  private void generatePrefilledCell(int colIndex, int rowIndex, List<String> items) {
    Cell cell = this.createCell(rowIndex, colIndex, "");
    if (items.size() == 1) {
      cell.setCellValue(items.stream().findFirst().orElseThrow());
      CellStyle lockedStyle = this.getWorkbook().createCellStyle();
      lockedStyle.setLocked(true);
      cell.setCellStyle(lockedStyle);
    }
  }

  /**
   * Generates the columnHeaders from the options provided in the {@link SamplesheetHeaderName} and
   * specifies the number of columns. Finally, it also specifies the width of a column dependent on
   * the selectable items within the cells of the column
   *
   * @param cellValueOptionsMap maps the selectable Items within the cells of a column with the
   *                            columnHeader
   */
  private void generateColumnsHeaders(LinkedHashMap<SamplesheetHeaderName,
      List<String>> cellValueOptionsMap) {
    List<Cell> headerCells = new ArrayList<>();
    setMaxColumns(SamplesheetHeaderName.values().length);
    for (SamplesheetHeaderName columnHeader : SamplesheetHeaderName.values()) {
      String columnLabel = columnHeader.label;
      int currentColumnIndex = columnHeader.ordinal();
      Cell cell = this.createCell(0, currentColumnIndex, columnLabel);
      headerCells.add(cell);
      List<String> cellValueOptions = cellValueOptionsMap.get(columnHeader);
      defineColumnWidthDependentOnLengthOfCellValueOptions(currentColumnIndex, columnLabel,
          Objects.requireNonNullElseGet(cellValueOptions, ArrayList::new));
    }
    styleColumnHeaderCells(headerCells);
  }


  /**
   * Updates the style of the header cells to be set to bold and locked.
   *
   * @param headerCells List containing the cells functioning as a header in the sheet which should
   *                    be assigned a bold and locked style
   */
  private void styleColumnHeaderCells(List<Cell> headerCells) {
    CellStyle boldHeaderStyle = this.getWorkbook().createCellStyle();
    Font font = this.getWorkbook().createFont();
    font.setBold(true);
    boldHeaderStyle.setFont(font);
    headerCells.forEach(cell -> cell.setCellStyle(boldHeaderStyle));
    //This has to be called separately since the reloadVisibleCellContent method starts the refresh at index 1
    refreshCells(headerCells);
  }

  /**
   * Defines the allocated width of a column based on the length of its label and the possible
   * cellValueOptions within a cell. Necessary since natively the spreadsheet.autofit() column
   * method does not account for the width of items within components within a cell
   *
   * @param colIndex         columnIndex for which the columnWidth should be calculated
   * @param colLabel         columnLabel equal to the one defined in the label of
   *                         {@link SamplesheetHeaderName}
   * @param cellValueOptions list of String values which are selectable within a cell.
   */
  private void defineColumnWidthDependentOnLengthOfCellValueOptions(int colIndex, String colLabel,
      List<String> cellValueOptions) {
    String longestColumnString = findLongestStringWithinColumn(colLabel, cellValueOptions);
    //Since all cells within a column have the same set of items we only need to set the value for one cell to allow autofit to find the correct width.
    Cell cell = this.getCell(0, colIndex);
    String oldValue = "";
    if (cell == null) {
      this.createCell(0, colIndex, longestColumnString);
    } else {
      oldValue = SpreadsheetMethods.cellToStringOrNull(cell);
      this.getCell(0, colIndex).setCellValue(longestColumnString);
    }
    try {
      this.autofitColumn(colIndex);
    } catch (IndexOutOfBoundsException exception) {
      throw new RuntimeException("Can't autofit column width due to" + exception.getMessage());
    }
    this.getCell(0, colIndex).setCellValue(oldValue);
  }


  /**
   * Finds and returns the longest String within the label and cellValueOptions within a column,
   * concatenated with a minimum spacer
   *
   * @param colLabel         columnLabel equal to the one defined in the label of *
   *                         {@link SamplesheetHeaderName}
   * @param cellValueOptions list of String values which are selectable within a cell.
   * @return Concatenation of the longest String within a column and a predefined spacer string.
   */
  private String findLongestStringWithinColumn(String colLabel, List<String> cellValueOptions) {
    //We need to ensure that there is a minimum Width for columns without cellValueOptions
    final String COL_SPACER = "___";
    List<String> stringList = new ArrayList<>(Collections.singletonList(colLabel));
    stringList.addAll(cellValueOptions);
    String longestString = stringList.stream().max(Comparator.comparingInt(String::length))
        .orElseThrow();
    return longestString + COL_SPACER;
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


  /**
   * Triggers the generation of the columnHeaders and cellValueOptions with componentRendering
   * specifically defined for the {@link MetadataType} of the genomics facility
   */
  private void generateGenomicsSheet() {
    header = retrieveGenomics();
    generateCellValueOptionsMap(header);
    generateColumnsHeaders(cellValueOptionsForColumnMap);
    dropdownCellFactory.setColumnValues(cellValueOptionsForColumnMap);
  }

  /**
   * Generates a LinkedHashMap containing an ordered collection of all possible cellValueOptions for
   * each Column of the {@link MetadataType} specific sheet. Necessary so the
   * {@link SpreadsheetDropdownFactory} knows which cells within a column should be rendered as
   * dropdown components and which should be default cells
   *
   * @param headerNames List of headerNames dependent on the selected {@link MetadataType}
   */
  private void generateCellValueOptionsMap(
      List<SamplesheetHeaderName> headerNames) {
    analysisTypes = generateGenomicsAnalysisMethods();
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
  }

  /**
   * Collects all {@link AnalysisMethod} specific for the genomic {@link MetadataType}
   *
   * @return List of String labels for all genomic analysis types.
   */
  private List<String> generateGenomicsAnalysisMethods() {
    return Arrays.stream(AnalysisMethod.values())
        .map(method -> "%s".formatted(method.label()))
        .sorted(Comparator.naturalOrder())
        .toList();
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

  /**
   * Collects the {@link SamplesheetHeaderName} specifically for the genomic {@link MetadataType}
   *
   * @return List of header names for the genomic {@link MetadataType} specific sheet
   */
  public List<SamplesheetHeaderName> retrieveGenomics() {
    return List.of(SamplesheetHeaderName.ROW, SamplesheetHeaderName.SEQ_ANALYSIS_TYPE,
        SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.ANALYTE,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  /**
   * Validates if the provided Input for each cell within the spreadsheet is valid Validation is
   * based on if the {@link SamplesheetHeaderName} mandatory attribute is set to true for the
   * selected cell. If that is the case, then the validation will trigger an error if the cell was
   * left blank
   *
   * @return {@link Result} containing nothing if the inputs are valid or
   * {@link InvalidSpreadsheetInput} if invalid input was detected.
   */
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

        if (!isCellValueValid(cell)) {
          invalidCells.add(cell);
        } else {
          // if a background color was set, but the cell is valid, we need to change the style
          if (cell.getCellStyle().getFillBackgroundColorColor() != null) {
            validCells.add(cell);
          }
        }
      }
    }
    //We need to reset the style for cells with valid content if they were previously invalid
    CellStyle defaultStyle = getDefaultStyle();
    validCells.forEach(cell -> cell.setCellStyle(defaultStyle));
    refreshCells(validCells);
    if (!invalidCells.isEmpty()) {
      highlightInvalidCells(invalidCells);
      return Result.fromError(new InvalidSpreadsheetInput(
          SpreadsheetInvalidationReason.MISSING_INPUT));
    }
    return Result.fromValue(null);
  }

  /**
   * Validates if the provided cell is in a column which requires mandatory information based on the
   * {@link SamplesheetHeaderName} mandatory attribute. If that is the case, then the validation
   * will trigger an error if the cell was left blank or if a value outside the possible selectable
   * values was provided
   *
   * @param cell to be investigated for validity
   * @return boolean showing if the provided cell is valid or not
   */
  private boolean isCellValueValid(Cell cell) {
    String cellValue = cell.getStringCellValue().trim();
    //Check if there are columns which require mandatory information
    List<SamplesheetHeaderName> mandatorySampleSheetHeaderNames = new ArrayList<>(
        header.stream().filter(samplesheetHeaderName -> samplesheetHeaderName.isMandatory)
            .toList());
    Optional<SamplesheetHeaderName> mandatoryHeaderName = mandatorySampleSheetHeaderNames.stream().
        filter(mandatorySampleSheetHeaderName -> mandatorySampleSheetHeaderName.ordinal()
            == cell.getColumnIndex()).findFirst();
    if (mandatoryHeaderName.isPresent()) {
      List<String> cellValueOptions = cellValueOptionsForColumnMap.get(mandatoryHeaderName.get());
      //If there are no options for the cell value the user has to provide some sort of information
      if (cellValueOptions.isEmpty()) {
        return !cellValue.isBlank();
      } else {
        //Otherwise the user should have selected one of the possible cell values
        return cellValueOptions.contains(cellValue);
      }
    }
    return true;
  }

  private CellStyle getDefaultStyle() {
    CellStyle defaultStyle = getWorkbook().createCellStyle();
    defaultStyle.setLocked(false);
    return defaultStyle;
  }


  private boolean areNullCellsInRow(Row row) {
    return StreamSupport.stream(row.spliterator(), false).anyMatch(Objects::isNull);
  }

  /**
   * Sets the cell Style of the provided cells to a predefined invalid style
   *
   * @param invalidCells cells in which the value provided did not pass the validation step
   */
  private void highlightInvalidCells(Collection<Cell> invalidCells) {
    CellStyle invalidStyle = this.getWorkbook().createCellStyle();
    invalidStyle.setLocked(false);

    ExtendedColor redErrorHue = SpreadsheetMethods.convertRGBToSpreadsheetColor(Color.red, 0.1);

    invalidStyle.setFillBackgroundColor(redErrorHue);

    for (Cell cell : invalidCells) {
      cell.setCellStyle(invalidStyle);
    }
    //We need to refresh the cells so the style change takes effect.
    refreshCells(invalidCells);
  }

  /**
   * Returns a List of {@link NGSRowDTO} for each row for which the least mandatory information was
   * provided by the user
   *
   * @return {@link NGSRowDTO} containing the provided mandatory specific information for the
   * genomic {@link MetadataType} sheet
   */
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
              commentInput.trim(), AnalysisMethod.forFixedTerm(analysisTypeInput.trim())));
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

  public void prefillConditionsAndReplicates() {
    int conditionColIndex = header.indexOf(SamplesheetHeaderName.CONDITION);
    int replicateColIndex = header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID);
    int rowIndex = 0;
    Set<String> conditions = conditionsToReplicates.keySet();
    for (String condition : conditions) {
      List<String> sortedLabels = conditionsToReplicates.get(condition).stream()
          .sorted(new LexicographicLabelComparator()).map(BiologicalReplicate::label).toList();
      for (String label : sortedLabels) {
        rowIndex++;
        Cell replicateCell = this.getCell(rowIndex, replicateColIndex);
        Cell conditionCell = this.getCell(rowIndex, conditionColIndex);
        replicateCell.setCellValue(label);
        conditionCell.setCellValue(condition);
      }
    }
  }

  /**
   * Record containing the provided mandatory specific information for the genomic
   * {@link MetadataType} sheet
   */

  public record NGSRowDTO(String analysisType, String sampleLabel,
                          BiologicalReplicateId bioReplicateID,
                          Long experimentalGroupId, String species, String specimen, String analyte,
                          String customerComment, AnalysisMethod analysisMethod) {

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

  /**
   * The InvalidSpreadsheetInput class is employed within the spreadsheet validation and contains
   * the information of the row for which the validation has failed
   * {@link SpreadsheetInvalidationReason} outlining why the validation has failed and additional
   * information if necessary
   *
   * @since 1.0.0
   */

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
