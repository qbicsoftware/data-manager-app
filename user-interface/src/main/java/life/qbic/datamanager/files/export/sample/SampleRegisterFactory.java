package life.qbic.datamanager.files.export.sample;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.datamanager.files.export.WorkbookFactory;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.sample.RegisterColumn;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class SampleRegisterFactory implements WorkbookFactory {

  private final List<String> analysisMethods;
  private final List<String> conditions;
  private final List<String> analytes;
  private final List<String> species;
  private final List<String> specimen;

  public SampleRegisterFactory(List<String> analysisMethods, List<String> conditions,
      List<String> analytes, List<String> species,
      List<String> specimen) {
    this.analysisMethods = analysisMethods;
    this.conditions = conditions;
    this.analytes = analytes;
    this.species = species;
    this.specimen = specimen;
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
    return RegisterColumn.values();
  }


  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.ANALYSIS.getIndex(),
        "Analysis Method",
        analysisMethods);

    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.CONDITION.getIndex(),
        "Condition",
        conditions);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.ANALYTE.getIndex(),
        "Analytes",
        analytes);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.SPECIES.getIndex(),
        "Species",
        species);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        RegisterColumn.SPECIMEN.getIndex(),
        "Specimen",
        specimen);
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (columnIndex == RegisterColumn.ANALYSIS.getIndex()) {
      return analysisMethods.stream().reduce(keepLongerString);
    } else if (columnIndex == RegisterColumn.CONDITION.getIndex()) {
      return conditions.stream().reduce(keepLongerString);
    } else if (columnIndex == RegisterColumn.ANALYTE.getIndex()) {
      return analytes.stream().reduce(keepLongerString);
    } else if (columnIndex == RegisterColumn.SPECIES.getIndex()) {
      return species.stream().reduce(keepLongerString);
    } else if (columnIndex == RegisterColumn.SPECIMEN.getIndex()) {
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
