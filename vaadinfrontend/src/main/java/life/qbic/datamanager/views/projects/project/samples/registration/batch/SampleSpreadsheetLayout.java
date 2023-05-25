package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.application.SampleRegistrationService.SamplesheetHeaderName;
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

/**
 * <b>Sample Spreadsheet Layout</b>
 * <p>
 * Layout which is responsible for hosting the spreadsheet in which the metadata information
 * associated for each sample will be provided>
 * </p>
 */
class SampleSpreadsheetLayout extends VerticalLayout {

  private final Span sampleInformationHeader = new Span("Sample Information");
  private final Span batchRegistrationInstruction = new Span();
  private final Label batchName = new Label();
  public final Spreadsheet sampleRegistrationSpreadsheet = new Spreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button addRowButton = new Button("Add Row");
  public final Button registerButton = new Button("Register");
  private final SampleRegistrationSheetBuilder sampleRegistrationSheetBuilder;
  private final SampleInformationLayoutHandler sampleInformationLayoutHandler;

  SampleSpreadsheetLayout(SampleRegistrationService sampleRegistrationService) {
    initContent();
    this.setSizeFull();
    sampleRegistrationSheetBuilder = new SampleRegistrationSheetBuilder(sampleRegistrationService);
    sampleInformationLayoutHandler = new SampleInformationLayoutHandler();
  }

  private void initContent() {
    initHeaderAndInstruction();
    add(sampleRegistrationSpreadsheet);
    styleSampleRegistrationSpreadSheet();
    initButtonLayout();
  }

  private void initHeaderAndInstruction() {
    sampleInformationHeader.addClassNames("text-xl", "font-bold", "text-secondary");
    batchRegistrationInstruction.add("Please register your samples for Batch: ");
    batchRegistrationInstruction.add(batchName);
    batchName.addClassNames(FontWeight.BOLD, FontWeight.BLACK);
    add(sampleInformationHeader);
    add(batchRegistrationInstruction);
  }

  private void initButtonLayout() {
    HorizontalLayout sampleInformationButtons = new HorizontalLayout();
    addRowButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
      @Override
      public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        sampleRegistrationSheetBuilder.addRows(sampleRegistrationSpreadsheet,
            sampleRegistrationSheetBuilder.findLastRow(sampleRegistrationSpreadsheet)+1);
      }
    });
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sampleInformationButtons.add(addRowButton, cancelButton, registerButton);
    this.setAlignSelf(Alignment.END, sampleInformationButtons);
    add(sampleInformationButtons);
  }

  private void styleSampleRegistrationSpreadSheet() {
    sampleRegistrationSpreadsheet.setSizeFull();
    sampleRegistrationSpreadsheet.setSheetSelectionBarVisible(false);
    sampleRegistrationSpreadsheet.setFunctionBarVisible(false);
  }

  public void generateSampleRegistrationSheet(MetadataType metaDataType) {
    sampleRegistrationSpreadsheet.reset();
    sampleRegistrationSheetBuilder.addSheetToSpreadsheet(metaDataType,
        sampleRegistrationSpreadsheet);
    sampleRegistrationSpreadsheet.reload();
  }

  public void reset() {
    sampleInformationLayoutHandler.reset();
  }

  public void setBatchName(String text) {
    batchName.setText(text);
  }

  public boolean isInputValid() {
    return sampleInformationLayoutHandler.isInputValid();
  }

  public void setActiveExperiment(Experiment experiment) {
    SampleRegistrationSheetBuilder.setExperimentMetadata(experiment);
  }

  public List<NGSRowDTO> getFilledRows(List<String> header) {
    List<NGSRowDTO> rows = new ArrayList<>();
    for(int i = 1; i < Integer.MAX_VALUE; i++) {
      Row row = sampleRegistrationSpreadsheet.getActiveSheet().getRow(i);
      Cell analysisTypeCell = row.getCell(header.indexOf(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE));
      Cell sampleLabelCell = row.getCell(header.indexOf(SamplesheetHeaderName.SAMPLE_LABEL));
      Cell replicateIDCell = row.getCell(header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID));
      Cell conditionCell = row.getCell(header.indexOf(SamplesheetHeaderName.CONDITION));
      Cell speciesCell = row.getCell(header.indexOf(SamplesheetHeaderName.SPECIES));
      Cell specimenCell = row.getCell(header.indexOf(SamplesheetHeaderName.SPECIMEN));
      Cell commentCell = row.getCell(header.indexOf(SamplesheetHeaderName.CUSTOMER_COMMENT));

      Supplier<Stream<Cell>> mandatoryCellStreamSupplier = () -> Stream.of(analysisTypeCell, sampleLabelCell,
          replicateIDCell, conditionCell, speciesCell, specimenCell);

      if (mandatoryCellStreamSupplier.get().anyMatch(Objects::isNull)) {
        break;
      }
      if(mandatoryCellStreamSupplier.get().noneMatch(x -> x.getStringCellValue().isEmpty())) {
        rows.add(new NGSRowDTO(analysisTypeCell.getStringCellValue().trim(), sampleLabelCell.getStringCellValue().trim(),
            replicateIDCell.getStringCellValue().trim(), conditionCell.getStringCellValue().trim(),
            speciesCell.getStringCellValue().trim(), specimenCell.getStringCellValue().trim(), commentCell.getStringCellValue().trim()));
      }
    }
    return rows;
  }

  public record NGSRowDTO(String analysisType, String sampleLabel, String bioReplicateID,
                          String condition, String species, String specimen,
                          String customerComment) {
  }

  private class SampleInformationLayoutHandler implements Serializable {

    @Serial
    private static final long serialVersionUID = 2837608401189525502L;
    private final List<Binder<?>> binders = new ArrayList<>();

    private void reset() {
      resetChildValues();
    }

    private void resetChildValues() {
      batchName.setText("");
      sampleRegistrationSpreadsheet.reset();
      sampleRegistrationSpreadsheet.reload();
    }

    private boolean isInputValid() {
      binders.forEach(Binder::validate);
      return binders.stream().allMatch(Binder::isValid);
    }
  }

  private static class SampleRegistrationSheetBuilder implements Serializable {

    @Serial
    private static final long serialVersionUID = 573778360298068552L;
    private final transient SampleRegistrationService sampleRegistrationService;

    private SpreadsheetDropdownFactory dropdownCellFactory;

    private List<SamplesheetHeaderName> header;
    private static List<String> species;
    private static List<String> specimens;
    private static Map<String, List<BiologicalReplicate>> conditionsToReplicates;
    private static int numberOfSamples;

    public SampleRegistrationSheetBuilder(SampleRegistrationService sampleRegistrationService) {
      this.sampleRegistrationService = sampleRegistrationService;
    }

    /**
     * Sets an experiment in order to provide the spreadsheet builder with known metadata
     * to prefill certain columns
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

    private static void prepareConditionItems(List<ExperimentalGroup> groups) {
      // create condition items for dropdown and fix cell width. Remember replicates for each condition
      conditionsToReplicates = new HashMap<>();
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
        conditionsToReplicates.put(conditionString, group.biologicalReplicates());
      }
    }

    private List<String> getReplicateLabels() {
      //TODO values should depend on selected condition!?
      List<String> replicateLabels = new ArrayList<>();
      for(List<BiologicalReplicate> replicates : conditionsToReplicates.values()) {
        replicateLabels.addAll(replicates.stream().map(BiologicalReplicate::label).toList());
      }
      return replicateLabels;
    }

    /**
     * Adds rows to the spreadsheet that contains prefilled data, selectable dropdowns and editable
     * free-text cells. The rows are added below the last row containing data.
     * @param spreadsheet the Spreadsheet object the row should be added to.
     * @param rowIndex the row index of the last row
     */
    public void addRows(Spreadsheet spreadsheet, int rowIndex) {

      for(int columnIndex = 0; columnIndex < header.size(); columnIndex++) {
        SamplesheetHeaderName colHeader = header.get(columnIndex);
        switch (colHeader) {
          case SPECIES -> prefillCellsToRow(columnIndex, rowIndex, species, spreadsheet);
          case SPECIMEN -> prefillCellsToRow(columnIndex, rowIndex, specimens, spreadsheet);
          case CONDITION -> prefillCellsToRow(columnIndex, rowIndex,
              conditionsToReplicates.keySet().stream().toList(), spreadsheet);
          case BIOLOGICAL_REPLICATE_ID -> prefillCellsToRow(columnIndex, rowIndex, getReplicateLabels(),
              spreadsheet);
          default -> {
            DropdownColumn column = dropdownCellFactory.getColumn(columnIndex);
            if(column!=null) {
              column.increaseToRow(rowIndex);
            }
          }
        }

        Cell newCell = spreadsheet.getCell(rowIndex, columnIndex);
        boolean hasData = !isCellEmpty(newCell);
        boolean hasDropdown = dropdownCellFactory.findColumnInRange(1, columnIndex)!=null;
        //cells need to be unlocked if they have no data/dropdown
        if(!hasData && !hasDropdown) {
          unlockCellsToRow(spreadsheet, rowIndex, columnIndex);
        }
      }
    }

    private int lastColumn() {
      return header.size();
    }

    private int findLastRow(Spreadsheet spreadsheet) {
      int res = 0;
      for(int row = 1; row <= Integer.MAX_VALUE; row++) {
        List<Cell> thisRow = new ArrayList<>();
        for(int col = 0; col <= header.size(); col++) {
          thisRow.add(spreadsheet.getCell(row, col));
        }
        if (thisRow.stream().allMatch(x -> x == null)) {
          break;
        }
        res = row;
      }
      return res;
    }

    /**
     * Generates and prefills the correct cell components dependent on already specified values.
     * @param colIndex
     * @param maxRow
     * @param items
     * @param spreadsheet
     */
    private void prefillCellsToRow(int colIndex, int maxRow, List<String> items, Spreadsheet spreadsheet) {
      if(items.size() == 1) {
        List<Cell> cells = new ArrayList<>();
        for(int row = 1; row <= maxRow; row++) {
          Cell cell = spreadsheet.createCell(row, colIndex, items.get(0));
          cells.add(cell);
        }
        spreadsheet.refreshCells(cells);
      } else {
        dropdownCellFactory.addDropDownCell(maxRow, colIndex);
      }
    }

    /**
     * Adds an active sheet to the spreadsheet, prefills and styles the header and adds as many rows
     * as the known sample size suggests.
     * @param metaDataType the MetaDataType describing what kind of spreadsheet should be created
     * @param spreadsheet the Spreadsheet object the metadata should be added to
     */
    public void addSheetToSpreadsheet(MetadataType metaDataType, Spreadsheet spreadsheet) {
      spreadsheet.setActiveSheetProtected("password-needed-to-lock");
      dropdownCellFactory = new SpreadsheetDropdownFactory();

      switch (metaDataType) {
        case PROTEOMICS -> addProteomicsSheet(spreadsheet, sampleRegistrationService.retrieveProteomics());
        case LIGANDOMICS -> addLigandomicsSheet(spreadsheet, sampleRegistrationService.retrieveLigandomics());
        case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(spreadsheet, sampleRegistrationService.retrieveGenomics());
        case METABOLOMICS -> addMetabolomicsSheet(spreadsheet, sampleRegistrationService.retrieveMetabolomics());
      }
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
      //initialise first rows based on known sample size
      addRows(spreadsheet, numberOfSamples);
    }

    private void setupCommonDropDownColumns() {
      initDropDownColumn(header.indexOf(SamplesheetHeaderName.SPECIES), species);
      initDropDownColumn(header.indexOf(SamplesheetHeaderName.SPECIMEN), specimens);
      initDropDownColumn(header.indexOf(SamplesheetHeaderName.CONDITION), conditionsToReplicates.keySet().stream().toList());
      initDropDownColumn(header.indexOf(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID), getReplicateLabels());
    }

    private void initDropDownColumn(int colIndex, List<String> items) {
      if(items.size() > 1) {
        DropdownColumn itemDropDown = new DropdownColumn();
        itemDropDown.withItems(items);
        itemDropDown.toRowIndex(0).atColIndex(colIndex);
        dropdownCellFactory.addDropdownColumn(itemDropDown);
      }
    }

    private boolean isCellEmpty(Cell cell) {
      return cell==null || cell.getStringCellValue().isEmpty();
    }

    private void unlockCellsToRow(Spreadsheet spreadsheet, int maxRow, int column) {
      List<Cell> cells = new ArrayList<>();
      CellStyle unLockedStyle = spreadsheet.getWorkbook().createCellStyle();
      unLockedStyle.setLocked(false);
      for(int row = 1; row <= maxRow; row++) {
        if(isCellEmpty(spreadsheet.getCell(row, column))) {
          Cell cell = spreadsheet.createCell(row, column, "");
          cell.setCellStyle(unLockedStyle);
          cells.add(cell);
        }
      }
      spreadsheet.refreshCells(cells);
    }

    private void prepareColumnHeaderAndWidth(Spreadsheet spreadsheet, LinkedHashMap<SamplesheetHeaderName,
        List<String>> headerToPresets) {
      CellStyle boldHeaderStyle = spreadsheet.getWorkbook().createCellStyle();
      Font font = spreadsheet.getWorkbook().createFont();
      font.setBold(true);
      boldHeaderStyle.setFont(font);

      List<Cell> updatedCells = new ArrayList<>();
      int columnIndex = 0;
      for (SamplesheetHeaderName columnHeader : headerToPresets.keySet()) {
        List<String> presets = headerToPresets.get(columnHeader);
        String columnLabel = columnHeader.label;
        if (presets == null) {
          fixColumnWidth(spreadsheet, columnIndex, columnLabel, new ArrayList<>());
        } else {
          fixColumnWidth(spreadsheet, columnIndex, columnLabel, presets);
        }
        Cell cell = spreadsheet.createCell(0, columnIndex, columnLabel);
        cell.setCellStyle(boldHeaderStyle);
        updatedCells.add(cell);
        columnIndex++;
      }
      spreadsheet.refreshCells(updatedCells);
    }

    private void prepareCommonSheetTasks(Spreadsheet spreadsheet) {
      LinkedHashMap<SamplesheetHeaderName, List<String>> headerToPresets = new LinkedHashMap<>();
      for(SamplesheetHeaderName label : header) {
        headerToPresets.put(label, new ArrayList<>());
      }
      headerToPresets.put(SamplesheetHeaderName.SPECIES, species);
      headerToPresets.put(SamplesheetHeaderName.SPECIMEN, specimens);
      headerToPresets.put(SamplesheetHeaderName.CONDITION, conditionsToReplicates.keySet().stream().toList());
      headerToPresets.put(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, getReplicateLabels());
      prepareColumnHeaderAndWidth(spreadsheet, headerToPresets);
      spreadsheet.reload();
      setupCommonDropDownColumns();
    }

    private void addProteomicsSheet(Spreadsheet spreadsheet, List<SamplesheetHeaderName> header) {
      this.header = header;
      prepareCommonSheetTasks(spreadsheet);
    }

    private void addMetabolomicsSheet(Spreadsheet spreadsheet, List<SamplesheetHeaderName> header) {
      this.header = header;
      prepareCommonSheetTasks(spreadsheet);
    }

    private void addLigandomicsSheet(Spreadsheet spreadsheet, List<SamplesheetHeaderName> header) {
      this.header = header;
      prepareCommonSheetTasks(spreadsheet);
    }

    private void addGenomicsSheet(Spreadsheet spreadsheet, List<SamplesheetHeaderName> header) {
      this.header = header;
      LinkedHashMap<SamplesheetHeaderName, List<String>> headerToPresets = new LinkedHashMap<>();
      for(SamplesheetHeaderName head : header) {
        headerToPresets.put(head, new ArrayList<>());
      }
      headerToPresets.put(SamplesheetHeaderName.SPECIES, species);
      headerToPresets.put(SamplesheetHeaderName.SPECIMEN, specimens);
      headerToPresets.put(SamplesheetHeaderName.CONDITION, conditionsToReplicates.keySet().stream().toList());
      headerToPresets.put(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE, Arrays.stream(SequenceAnalysisType
              .values())
          .map(e -> e.label)
          .collect(Collectors.toList()));
      headerToPresets.put(SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, getReplicateLabels());
      prepareColumnHeaderAndWidth(spreadsheet, headerToPresets);
      spreadsheet.reload();
      DropdownColumn analysisTypeColumn = new DropdownColumn().withItems(Arrays.stream(SequenceAnalysisType
              .values())
          .map(e -> e.label)
          .collect(Collectors.toList()));
      analysisTypeColumn.toRowIndex(0)
          .atColIndex(header.indexOf(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE));

      dropdownCellFactory.addDropdownColumn(analysisTypeColumn);
      setupCommonDropDownColumns();
    }

    /*
     * Changes width of a spreadsheet column based on header element and potential known entries.
     */
    private void fixColumnWidth(Spreadsheet spreadsheet, int colIndex, String colLabel, List<String> entries) {
      final String COL_SPACER = "___";
      List<String> stringList = new ArrayList<>(Arrays.asList(colLabel));
      stringList.addAll(entries);
      String longestString = stringList.stream().max(Comparator.comparingInt(String::length))
          .get();
      String spacingValue = longestString+COL_SPACER;
      Cell cell = spreadsheet.getCell(0, colIndex);
      String oldValue = "";
      if(cell==null) {
        spreadsheet.createCell(0, colIndex, spacingValue);
      } else {
        oldValue = cell.getStringCellValue();
        spreadsheet.getCell(0, colIndex).setCellValue(spacingValue);
      }
      spreadsheet.autofitColumn(colIndex);
      spreadsheet.getCell(0, colIndex).setCellValue(oldValue);
    }
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

}
