package life.qbic.datamanager.files.export.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.datamanager.files.export.WorkbookFactory;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.sample.ConfoundingVariableColumn;
import life.qbic.datamanager.files.structure.sample.RegisterColumn;
import life.qbic.datamanager.views.general.confounding.ConfoundingVariable;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class SampleRegisterFactory implements WorkbookFactory {

  private final List<String> analysisMethods;
  private final List<String> conditions;
  private final List<String> analytes;
  private final List<String> species;
  private final List<String> specimen;
  private final List<ConfoundingVariable> confoundingVariables;

  public SampleRegisterFactory(List<String> analysisMethods, List<String> conditions,
      List<String> analytes, List<String> species,
      List<String> specimen, List<ConfoundingVariable> confoundingVariables) {
    this.analysisMethods = analysisMethods;
    this.conditions = conditions;
    this.analytes = analytes;
    this.species = species;
    this.specimen = specimen;
    this.confoundingVariables = confoundingVariables;
  }

  @Override
  public int numberOfRowsToGenerate() {
    return 2_000;
  }

  @Override
  public void enterValuesAsRows(Sheet sheet, CellStyles cellStyles) {
    //nothing to do
  }

  @Override
  public String sheetName() {
    return "Sample Metadata";
  }

  @Override
  public Column[] getColumns() {
    ArrayList<Column> columns = new ArrayList<>(List.of(RegisterColumn.values()));

    var colOffset = RegisterColumn.maxColumnIndex() + 1; //offset + 0 is the next free column
    for (int i = 0; i < confoundingVariables.size(); i++) {
      var confoundingVariable = confoundingVariables.get(i);
      int columnIndex = colOffset + i;
      columns.add(
          new ConfoundingVariableColumn(confoundingVariable.variableReference(), columnIndex,
              confoundingVariable.name()));
    }

    return columns.toArray(new Column[0]);
  }


  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.ANALYSIS.index(),
        "Analysis Method",
        analysisMethods);

    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.CONDITION.index(),
        "Condition",
        conditions);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.ANALYTE.index(),
        "Analytes",
        analytes);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.SPECIES.index(),
        "Species",
        species);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.SPECIMEN.index(),
        "Specimen",
        specimen);
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (columnIndex == RegisterColumn.ANALYSIS.index()) {
      return analysisMethods.stream().reduce(keepLongerString);
    } else if (columnIndex == RegisterColumn.CONDITION.index()) {
      return conditions.stream().reduce(keepLongerString);
    } else if (columnIndex == RegisterColumn.ANALYTE.index()) {
      return analytes.stream().reduce(keepLongerString);
    } else if (columnIndex == RegisterColumn.SPECIES.index()) {
      return species.stream().reduce(keepLongerString);
    } else if (columnIndex == RegisterColumn.SPECIMEN.index()) {
      return specimen.stream().reduce(keepLongerString);
    }
    return Optional.empty();
  }


  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    // nothing to do
  }
}
