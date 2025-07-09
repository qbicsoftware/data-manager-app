package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.datamanager.files.export.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ProteomicsWorkbooks.DigestionMethod;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementRegisterColumn;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ProteomicsRegisterFactory implements WorkbookFactory {

  private static final int DEFAULT_GENERATED_ROW_COUNT = 2000;

  @Override
  public int numberOfRowsToGenerate() {
    return DEFAULT_GENERATED_ROW_COUNT;
  }

  @Override
  public void enterValuesAsRows(Sheet sheet, CellStyles cellStyles) {
    //register case so no rows to enter
  }

  @Override
  public String sheetName() {
    return "Proteomics Measurement Metadata";
  }

  @Override
  public Column[] getColumns() {
    return ProteomicsMeasurementRegisterColumn.values();
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    WorkbookFactory.addValidation(
        hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        ProteomicsMeasurementRegisterColumn.DIGESTION_METHOD.index(),
        "Digestion method", DigestionMethod.getOptions()
    );
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (ProteomicsMeasurementRegisterColumn.DIGESTION_METHOD.index() == columnIndex) {
      return DigestionMethod.getOptions().stream().reduce(keepLongerString);
    }
    return Optional.empty();
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        ProteomicsMeasurementRegisterColumn.ORGANISATION_URL.index(), "https://ror.org");
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        ProteomicsMeasurementRegisterColumn.MS_DEVICE.index(),
        "https://rdm.qbic.uni-tuebingen.de");
  }
}
