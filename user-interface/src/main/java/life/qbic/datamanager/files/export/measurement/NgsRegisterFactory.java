package life.qbic.datamanager.files.export.measurement;

import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.datamanager.files.export.WorkbookFactory;
import life.qbic.datamanager.files.export.measurement.NGSWorkbooks.SequencingReadType;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.measurement.NGSMeasurementRegisterColumn;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class NgsRegisterFactory implements WorkbookFactory {

  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;

  @Override
  public Column[] getColumns() {
    return NGSMeasurementRegisterColumn.values();
  }

  @Override
  public int numberOfRowsToGenerate() {
    return DEFAULT_GENERATED_ROW_COUNT;
  }

  @Override
  public void enterValuesAsRows(Sheet sheet, CellStyles cellStyles) {
    // we do not need to enter anything
  }

  @Override
  public String sheetName() {
    return "NGS Measurement Metadata";
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    WorkbookFactory.addValidation(hiddenSheet, sheet, 1, numberOfRowsToGenerate() - 1,
        NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.index(),
        "Sequencing read type", SequencingReadType.getOptions());
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.index() == columnIndex) {
      return SequencingReadType.getOptions().stream().reduce(keepLongerString);
    }
    return Optional.empty();

  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        NGSMeasurementRegisterColumn.ORGANISATION_URL.index(), "https://ror.org");
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        NGSMeasurementRegisterColumn.INSTRUMENT.index(), "https://rdm.qbic.uni-tuebingen.de");
  }
}
