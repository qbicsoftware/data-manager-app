package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleSpreadsheetLayout.SequenceAnalysisType;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
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
  private static Map<String, List<BiologicalReplicate>> conditionsToReplicates;
  private static int numberOfSamples;
  private Sheet sampleRegistrationSheet;

  public SampleRegistrationSpreadsheet() {

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
    List<ExperimentalGroup> groups = experiment.getExperimentalGroups().stream().toList();
    numberOfSamples = groups.stream().map(ExperimentalGroup::sampleSize)
        .mapToInt(Integer::intValue).sum();
    prepareConditionItems(groups);
  }

  public void addSheetToSpreadsheet(MetadataType metaDataType) {

    dropdownCellFactory = new SpreadsheetDropdownFactory();

    switch (metaDataType) {
      case PROTEOMICS -> addProteomicsSheet(retrieveProteomics());
      case LIGANDOMICS -> addLigandomicsSheet(retrieveLigandomics());
      case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(retrieveGenomics());
      case METABOLOMICS -> addMetabolomicsSheet(retrieveMetabolomics());
    }
    //ToDo move sheet Creation to new Method since we know the header size
    this.createNewSheet("WAW", 5, 5);
    sampleRegistrationSheet = this.getActiveSheet();
    this.setDefaultColumnCount(header.size());
    this.setDefaultRowCount(numberOfSamples);
    this.setActiveSheetProtected("password-needed-to-lock");

    this.setSpreadsheetComponentFactory(dropdownCellFactory);
    //initialise first rows based on known sample size
    addRows(numberOfSamples);
  }

  private static void prepareConditionItems(List<ExperimentalGroup> groups) {
    // create condition items for dropdown and fix cell width. Remember replicates for each condition
    conditionsToReplicates = new HashMap<>();
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
      conditionsToReplicates.put(conditionString, group.biologicalReplicates());
    }
  }

  private List<String> getReplicateLabels() {
    //TODO values should depend on selected condition!?
    List<String> replicateLabels = new ArrayList<>();
    for (List<BiologicalReplicate> replicates : conditionsToReplicates.values()) {
      replicateLabels.addAll(replicates.stream().map(BiologicalReplicate::label).toList());
    }
    return replicateLabels;
  }

  /**
   * Adds rows to the spreadsheet that contains prefilled data, selectable dropdowns and editable
   * free-text cells. The rows are added below the last row containing data.
   *
   * @param rowIndex the row index of the last row
   */
  public void addRows(int rowIndex) {

    for (int columnIndex = 0; columnIndex < header.size(); columnIndex++) {
      SamplesheetHeaderName colHeader = header.get(columnIndex);
      switch (colHeader) {
        case SPECIES -> prefillCellsToRow(columnIndex, rowIndex, species, this);
        case SPECIMEN -> prefillCellsToRow(columnIndex, rowIndex, specimens, this);
        case CONDITION -> prefillCellsToRow(columnIndex, rowIndex,
            conditionsToReplicates.keySet().stream().toList(), this);
        case BIOLOGICAL_REPLICATE_ID ->
            prefillCellsToRow(columnIndex, rowIndex, getReplicateLabels(),
                this);
        default -> {
          DropdownColumn column = dropdownCellFactory.getColumn(columnIndex);
          if (column != null) {
            column.increaseToRow(rowIndex);
          }
        }
      }

      Cell newCell = this.getCell(rowIndex, columnIndex);
      boolean hasData = !this.isCellEmpty(newCell);
      boolean hasDropdown = dropdownCellFactory.findColumnInRange(1, columnIndex) != null;
      //cells need to be unlocked if they have no data/dropdown
      if (!hasData && !hasDropdown) {
        this.unlockCellsToRow(this, rowIndex, columnIndex);
      }
    }
  }


  /**
   * Generates and prefills the correct cell components dependent on already specified values.
   *
   * @param colIndex
   * @param maxRow
   * @param items
   * @param spreadsheet the Spreadsheet object the metadata should be added to
   */
  private void prefillCellsToRow(int colIndex, int maxRow, List<String> items,
      Spreadsheet spreadsheet) {
    if (items.size() == 1) {
      List<Cell> cells = new ArrayList<>();
      for (int row = 1; row <= maxRow; row++) {
        Cell cell = spreadsheet.createCell(row, colIndex, items.get(0));
        cells.add(cell);
      }
      spreadsheet.refreshCells(cells);
    } else {
      dropdownCellFactory.addDropDownCell(maxRow, colIndex);
    }
  }

  private void setupCommonDropDownColumns() {
    initDropDownColumn(header.indexOf(SamplesheetHeaderName.SPECIES), species);
    initDropDownColumn(header.indexOf(SamplesheetHeaderName.SPECIMEN), specimens);
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
    headerToPresets.put(SamplesheetHeaderName.CONDITION,
        conditionsToReplicates.keySet().stream().toList());
    headerToPresets.put(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, getReplicateLabels());
    prepareColumnHeaderAndWidth(headerToPresets);
    this.reload();
    setupCommonDropDownColumns();
  }


  public int findLastRow(Spreadsheet spreadsheet) {
    int res = 0;
    for (int row = 1; row <= Integer.MAX_VALUE; row++) {
      List<Cell> thisRow = new ArrayList<>();
      for (int col = 0; col <= header.size(); col++) {
        thisRow.add(spreadsheet.getCell(row, col));
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
  }

  private void addMetabolomicsSheet(List<SamplesheetHeaderName> header) {
    this.header = header;
    prepareCommonSheetTasks();
  }

  private void addLigandomicsSheet(List<SamplesheetHeaderName> header) {
    this.header = header;
    prepareCommonSheetTasks();
  }

  private void addGenomicsSheet(List<SamplesheetHeaderName> header) {
    this.header = header;
    LinkedHashMap<SamplesheetHeaderName, List<String>> headerToPresets = new LinkedHashMap<>();
    for (SamplesheetHeaderName head : header) {
      headerToPresets.put(head, new ArrayList<>());
    }
    headerToPresets.put(SamplesheetHeaderName.SPECIES, species);
    headerToPresets.put(SamplesheetHeaderName.SPECIMEN, specimens);
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
        "Condition"), SPECIES("Species"), SPECIMEN("Specimen"), CUSTOMER_COMMENT(
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
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveLigandomics() {
    return List.of(SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveMetabolomics() {
    return List.of(SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveGenomics() {
    return List.of(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE, SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  private List<NGSRowDTO> getFilledRows() {
    List<NGSRowDTO> rows = new ArrayList<>();
    for (int i = 1; i < this.getLastRow(); i++) {
      Row row = this.getActiveSheet().getRow(i);
      Cell analysisTypeCell = row.getCell(header.indexOf(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE));
      Cell sampleLabelCell = row.getCell(header.indexOf(SamplesheetHeaderName.SAMPLE_LABEL));
      Cell replicateIDCell = row.getCell(
          header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID));
      Cell conditionCell = row.getCell(header.indexOf(SamplesheetHeaderName.CONDITION));
      Cell speciesCell = row.getCell(header.indexOf(SamplesheetHeaderName.SPECIES));
      Cell specimenCell = row.getCell(header.indexOf(SamplesheetHeaderName.SPECIMEN));
      Cell commentCell = row.getCell(header.indexOf(SamplesheetHeaderName.CUSTOMER_COMMENT));

      Supplier<Stream<Cell>> mandatoryCellStreamSupplier = () -> Stream.of(analysisTypeCell,
          sampleLabelCell,
          replicateIDCell, conditionCell, speciesCell, specimenCell);

      if (mandatoryCellStreamSupplier.get().anyMatch(Objects::isNull)) {
        break;
      }
      if (mandatoryCellStreamSupplier.get().noneMatch(x -> x.getStringCellValue().isEmpty())) {
        rows.add(new NGSRowDTO(analysisTypeCell.getStringCellValue().trim(),
            sampleLabelCell.getStringCellValue().trim(),
            replicateIDCell.getStringCellValue().trim(), conditionCell.getStringCellValue().trim(),
            speciesCell.getStringCellValue().trim(), specimenCell.getStringCellValue().trim(),
            commentCell.getStringCellValue().trim()));
      }
    }
    return rows;
  }

  public record NGSRowDTO(String analysisType, String sampleLabel, String bioReplicateID,
                          String condition, String species, String specimen,
                          String customerComment) {

  }
}
