package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.data.binder.Binder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  public final Button addRowButton = new Button("Add Row");
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
    addRowButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
      @Override
      public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        sampleRegistrationSheetBuilder.addRow(sampleRegistrationSpreadsheet);
      }
    });
    sampleMetadataButtons.add(addRowButton, cancelButton, nextButton);
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
    SampleRegistrationSheetBuilder.setExperimentMetadata(experiment);
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

    private static SpreadsheetDropdownFactory dropdownCellFactory;

    private static List<String> header;
    private static List<String> species;
    private static List<String> specimens;
    private static List<String> conditions;
    private static int numberOfSamples;

    public SampleRegistrationSheetBuilder(SampleRegistrationService sampleRegistrationService) {
      this.sampleRegistrationService = sampleRegistrationService;
    }

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
      // create condition items for dropdown and fix cell width
      conditions = new ArrayList<>();
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
        conditions.add(conditionString);
      }
    }

    public void addRow(Spreadsheet spreadsheet) {
      int rowIndex = findLastRow(spreadsheet)+1;

      for(int columnIndex = 0; columnIndex < header.size(); columnIndex++) {
        String colHeader = header.get(columnIndex);
        switch (colHeader) {
          case "Species" -> prefillCell(columnIndex, rowIndex, species, spreadsheet);
          case "Specimen" -> prefillCell(columnIndex, rowIndex, specimens, spreadsheet);
          case "Condition" -> prefillCell(columnIndex, rowIndex, conditions, spreadsheet);
          default -> {
            DropDownColumn column = dropdownCellFactory.getColumn(columnIndex);
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
          unlockCell(spreadsheet, rowIndex, columnIndex);
        }
      }
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
     * Used to generate cell for Species, Specimen and Condition
     * @param colIndex
     * @param rowIndex
     * @param items
     * @param spreadsheet
     */
    private void prefillCell(int colIndex, int rowIndex, List<String> items, Spreadsheet spreadsheet) {
      if(items.size() == 1) {
        Cell cell = spreadsheet.createCell(rowIndex, colIndex, items.get(0));
        spreadsheet.refreshCells(cell);
      } else {
        dropdownCellFactory.addDropDownCell(rowIndex, colIndex);
      }
    }

    public void addSheetToSpreadsheet(MetaDataTypes metaDataTypes, Spreadsheet spreadsheet) {
      spreadsheet.setActiveSheetProtected("password-needed-to-lock");
      dropdownCellFactory = new SpreadsheetDropdownFactory();

      switch (metaDataTypes) {
        case PROTEOMICS -> addProteomicsSheet(spreadsheet, sampleRegistrationService.retrieveProteomics());
        case LIGANDOMICS -> addLigandomicsSheet(spreadsheet, sampleRegistrationService.retrieveLigandomics());
        case TRANSCRIPTOMICS_GENOMICS -> addGenomicsSheet(spreadsheet, sampleRegistrationService.retrieveGenomics());
        case METABOLOMICS -> addMetabolomicsSheet(spreadsheet, sampleRegistrationService.retrieveMetabolomics());
      }
      spreadsheet.setSpreadsheetComponentFactory(dropdownCellFactory);
      //initialise first rows based on known sample size
      for(int rowIndex = 1; rowIndex <= numberOfSamples; rowIndex++) {
        addRow(spreadsheet);
      }
    }

    private void setupCommonDropDownColumns() {
      initDropDownColumn(header.indexOf("Species"), species);
      initDropDownColumn(header.indexOf("Specimen"), specimens);
      initDropDownColumn(header.indexOf("Condition"), conditions);
    }

    private void initDropDownColumn(int colIndex, List<String> items) {
      if(items.size() > 1) {
        DropDownColumn itemDropDown = new DropDownColumn();
        itemDropDown.withItems(items);
        itemDropDown.toRowIndex(0).atColIndex(colIndex);
        dropdownCellFactory.addDropdownColumn(itemDropDown);
      }
    }

    private void unlockEmptyCells(Spreadsheet spreadsheet) {
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
          unlockCell(spreadsheet, column, 1);
        }
      }
    }

    private boolean isCellEmpty(Cell cell) {
      return cell==null || cell.getStringCellValue().isEmpty();
    }

    private void unlockCell(Spreadsheet spreadsheet, int row, int column) {
      CellStyle unLockedStyle = spreadsheet.getWorkbook().createCellStyle();
      unLockedStyle.setLocked(false);
      Cell cell = spreadsheet.createCell(row, column, "");
      cell.setCellStyle(unLockedStyle);
      spreadsheet.refreshCells(cell);
    }

    private void prepareColumnHeaderAndWidth(Spreadsheet spreadsheet, LinkedHashMap<String,
        List<String>> headerToPresets) {
      CellStyle boldHeaderStyle = spreadsheet.getWorkbook().createCellStyle();
      Font font = spreadsheet.getWorkbook().createFont();
      font.setBold(true);
      boldHeaderStyle.setFont(font);

      List<Cell> updatedCells = new ArrayList<>();
      int columnIndex = 0;
      for (String columnHeader : headerToPresets.keySet()) {
        List<String> presets = headerToPresets.get(columnHeader);
        if(presets==null) {
          fixColumnWidth(spreadsheet, columnIndex, columnHeader, new ArrayList<>());
        } else {
          fixColumnWidth(spreadsheet, columnIndex, columnHeader, presets);
        }
        Cell cell = spreadsheet.createCell(0, columnIndex, columnHeader);
        cell.setCellStyle(boldHeaderStyle);
        updatedCells.add(cell);
        columnIndex++;
      }
      fixColumnWidth(spreadsheet, header.indexOf("Species"), "Species", species);
      fixColumnWidth(spreadsheet, header.indexOf("Specimen"), "Specimen", specimens);
      fixColumnWidth(spreadsheet, header.indexOf("Condition"), "Condition", conditions);

      spreadsheet.refreshCells(updatedCells);
    }

    private void prepareCommonSheetTasks(Spreadsheet spreadsheet) {
      LinkedHashMap<String, List<String>> headerToPresets = new LinkedHashMap<>();
      for(String head : header) {
        headerToPresets.put(head, new ArrayList<>());
      }
      headerToPresets.put("Species", species);
      headerToPresets.put("Specimen", specimens);
      headerToPresets.put("Condition", conditions);
      prepareColumnHeaderAndWidth(spreadsheet, headerToPresets);
      spreadsheet.reload();
      setupCommonDropDownColumns();
    }

    private void addProteomicsSheet(Spreadsheet spreadsheet, List<String> header) {
      this.header = header;
      prepareCommonSheetTasks(spreadsheet);

    }

    private void addMetabolomicsSheet(Spreadsheet spreadsheet, List<String> header) {
      this.header = header;
      prepareCommonSheetTasks(spreadsheet);
    }

    private void addLigandomicsSheet(Spreadsheet spreadsheet, List<String> header) {
      this.header = header;
      prepareCommonSheetTasks(spreadsheet);
    }

    private void addGenomicsSheet(Spreadsheet spreadsheet, List<String> header) {
      this.header = header;
      LinkedHashMap<String, List<String>> headerToPresets = new LinkedHashMap<>();
      for(String head : header) {
        headerToPresets.put(head, new ArrayList<>());
      }
      headerToPresets.put("Species", species);
      headerToPresets.put("Specimen", specimens);
      headerToPresets.put("Condition", conditions);
      headerToPresets.put("Analysis to be performed", Arrays.stream(SequenceAnalysisTypes
              .values())
          .map(e -> e.label)
          .collect(Collectors.toList()));
      prepareColumnHeaderAndWidth(spreadsheet, headerToPresets);
      spreadsheet.reload();
      DropDownColumn techColumn = new DropDownColumn().withItems(Arrays.stream(SequenceAnalysisTypes
              .values())
          .map(e -> e.label)
          .collect(Collectors.toList()));
      techColumn.toRowIndex(0)
          .atColIndex(header.indexOf("Analysis to be performed"));

      dropdownCellFactory.addDropdownColumn(techColumn);
      setupCommonDropDownColumns();
    }

    /*
     * Changes width of a spreadsheet column based on header element and potential known entries.
     */
    private void fixColumnWidth(Spreadsheet spreadsheet, int colIndex, String colHeader, List<String> entries) {
      final String COL_SPACER = "___";
      List<String> stringList = new ArrayList<>(Arrays.asList(colHeader));
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
