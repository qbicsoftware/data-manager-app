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
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicate;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    for (int currentRow = 1; currentRow < numberOfSamples; currentRow++) {
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
    List<String> replicateLabels = new ArrayList<>();
    for (List<BiologicalReplicate> replicates : conditionsToReplicates.values()) {
      replicateLabels.addAll(replicates.stream().map(BiologicalReplicate::label).toList());
    }
    return replicateLabels.stream().distinct().toList();
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
      boolean hasData = !this.isCellEmpty(newCell);
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
      if (isCellEmpty(spreadsheet.getCell(row, column))) {
        Cell cell = spreadsheet.createCell(row, column, "");
        cell.setCellStyle(unLockedStyle);
        cells.add(cell);
      }
    }
    spreadsheet.refreshCells(cells);
  }

  boolean isCellEmpty(Cell cell) {
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
    SEQ_ANALYSIS_TYPE("Analysis to be performed"), SAMPLE_LABEL(
        "Sample label"), BIOLOGICAL_REPLICATE_ID("Biological replicate id"), CONDITION(
        "Condition"), SPECIES("Species"), SPECIMEN("Specimen"), ANALYTE(
        "Analyte"), CUSTOMER_COMMENT(
        "Customer comment");
    public final String label;

    SamplesheetHeaderName(String label) {
      this.label = label;
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

  public Result<List<NGSRowDTO>, SpreadsheetValidationException> getFilledRows() {
    List<NGSRowDTO> rows = new ArrayList<>();
    Set<String> uniqueSamples = new HashSet<>();
    for (int rowId = 1; rowId < sampleRegistrationSheet.getLastRowNum(); rowId++) {
      Row row = sampleRegistrationSheet.getRow(rowId);

      String analysisTypeInput = parseAndTrimCellOfRow(row, header.indexOf(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE));
      String sampleLabelInput = parseAndTrimCellOfRow(row, header.indexOf(SamplesheetHeaderName.SAMPLE_LABEL));
      String replicateIDInput = parseAndTrimCellOfRow(row, header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID));
      String conditionInput = parseAndTrimCellOfRow(row, header.indexOf(SamplesheetHeaderName.CONDITION));
      String speciesInput = parseAndTrimCellOfRow(row, header.indexOf(SamplesheetHeaderName.SPECIES));
      String specimenInput = parseAndTrimCellOfRow(row, header.indexOf(SamplesheetHeaderName.SPECIMEN));
      String analyteInput = parseAndTrimCellOfRow(row, header.indexOf(SamplesheetHeaderName.ANALYTE));
      String commentInput = parseAndTrimCellOfRow(row, header.indexOf(SamplesheetHeaderName.CUSTOMER_COMMENT));

      // we need to stream this list twice, so we use a supplier
      Supplier<Stream<String>> mandatoryCellStreamSupplier = () -> Stream.of(analysisTypeInput,
          sampleLabelInput,
          replicateIDInput, conditionInput, speciesInput, specimenInput, analyteInput);

      if (mandatoryCellStreamSupplier.get().anyMatch(Objects::isNull)) {
        break;
      }

      if(mandatoryCellStreamSupplier.get().noneMatch(x -> x.isEmpty())) {
        String uniqueSampleString = replicateIDInput+conditionInput;
        if(uniqueSamples.contains(uniqueSampleString)) {
          return Result.fromError(new SpreadsheetValidationException(
              "Biological replicate Id "+replicateIDInput+" was used multiple times for the same condition.", rowId));
        } else {
          ExperimentalGroup experimentalGroup = experimentalGroupToConditionString.get(
              conditionInput);
          Long experimentalGroupId = experimentalGroup.id();
          BiologicalReplicateId biologicalReplicateId = retrieveBiologicalReplicateId(
              replicateIDInput, conditionInput);
          rows.add(new NGSRowDTO(analysisTypeInput, sampleLabelInput,
              biologicalReplicateId, experimentalGroupId,
              speciesInput, specimenInput, analyteInput,
              commentInput));
          uniqueSamples.add(uniqueSampleString);
        }
      } else {
        return Result.fromError(new SpreadsheetValidationException(
            "Missing mandatory fields.", rowId));
      }

    }
    return Result.fromValue(rows);
  }

  private String parseAndTrimCellOfRow(Row row, int colId) {
    Cell cell = row.getCell(colId);
    if(cell==null) {
      return null;
    }
    switch (cell.getCellType()) {
      case STRING -> {
        return cell.getStringCellValue().trim();
      }
      case NUMERIC -> {
        double dbl = cell.getNumericCellValue();
        if((dbl % 1) == 0) {
          int integer = (int) Math.floor(dbl);
          return Integer.toString(integer);
        } else {
          return Double.toString(dbl);
        }
      }
      default -> {
        return null;
      }
    }
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
                          String customerComment) {}

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

  public static class SpreadsheetValidationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = 3532483874741500810L;

    private final int invalidRow;

    SpreadsheetValidationException(String message, int invalidRow) {
      super(message);
      this.invalidRow = invalidRow;
    }

    public int getInvalidRow() {
      return invalidRow;
    }
  }

}
