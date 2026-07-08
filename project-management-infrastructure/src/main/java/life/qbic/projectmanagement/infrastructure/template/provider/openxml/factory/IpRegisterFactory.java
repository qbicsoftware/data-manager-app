package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.IPMeasurementRegisterColumn;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class IpRegisterFactory implements WorkbookFactory {

  private static final int DEFAULT_GENERATED_ROW_COUNT = 2000;

  @Override
  public Column[] getColumns() {
    return IPMeasurementRegisterColumn.values();
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
    return "IP Measurement Metadata";
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    // No special validations for IP measurements currently
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    // No specific column needs longest value currently
    return Optional.empty();
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        IPMeasurementRegisterColumn.ORGANISATION_URL.index(), "https://ror.org");
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        IPMeasurementRegisterColumn.INSTRUMENT.index(), "https://rdm.qbic.uni-tuebingen.de");
  }
}
