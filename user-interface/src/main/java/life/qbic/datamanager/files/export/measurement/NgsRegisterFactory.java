package life.qbic.datamanager.files.export.measurement;

import life.qbic.datamanager.files.export.measurement.NGSWorkbooks.SequencingReadType;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.measurement.NGSMeasurementRegisterColumn;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class NgsRegisterFactory implements MeasurementWorkbookFactory {

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
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    MeasurementWorkbookFactory.addValidation(hiddenSheet, sheet, 1, numberOfRowsToGenerate() - 1,
        NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.getIndex(),
        "Sequencing read type", SequencingReadType.getOptions());
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    MeasurementWorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        NGSMeasurementRegisterColumn.ORGANISATION_URL.getIndex(), "https://ror.org");
    MeasurementWorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        NGSMeasurementRegisterColumn.INSTRUMENT.getIndex(), "https://rdm.qbic.uni-tuebingen.de");
  }
}
