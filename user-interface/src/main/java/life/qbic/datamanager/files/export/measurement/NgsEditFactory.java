package life.qbic.datamanager.files.export.measurement;

import static life.qbic.datamanager.files.export.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.getOrCreateRow;

import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.files.export.measurement.NGSWorkbooks.SequencingReadType;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.measurement.NGSMeasurementEditColumn;
import life.qbic.datamanager.views.projects.project.measurements.NGSMeasurementEntry;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class NgsEditFactory implements MeasurementWorkbookFactory {

  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;
  private final List<NGSMeasurementEntry> measurements;

  public NgsEditFactory(List<NGSMeasurementEntry> measurements) {
    this.measurements = Objects.requireNonNull(measurements);
  }


  private static void writeMeasurementIntoRow(NGSMeasurementEntry ngsMeasurementEntry,
      Row entryRow, CellStyle defaultStyle, CellStyle readOnlyCellStyle) {
    for (NGSMeasurementEditColumn measurementColumn : NGSMeasurementEditColumn.values()) {
      var value = switch (measurementColumn) {
        case MEASUREMENT_ID -> ngsMeasurementEntry.measurementCode();
        case SAMPLE_ID -> ngsMeasurementEntry.sampleInformation().sampleId();
        case SAMPLE_NAME -> ngsMeasurementEntry.sampleInformation().sampleName();
        case POOL_GROUP -> ngsMeasurementEntry.samplePoolGroup();
        case ORGANISATION_URL -> ngsMeasurementEntry.organisationId();
        case ORGANISATION_NAME -> ngsMeasurementEntry.organisationName();
        case FACILITY -> ngsMeasurementEntry.facility();
        case INSTRUMENT -> ngsMeasurementEntry.instrumentCURI();
        case INSTRUMENT_NAME -> ngsMeasurementEntry.instrumentName();
        case SEQUENCING_READ_TYPE -> ngsMeasurementEntry.readType();
        case LIBRARY_KIT -> ngsMeasurementEntry.libraryKit();
        case FLOW_CELL -> ngsMeasurementEntry.flowCell();
        case SEQUENCING_RUN_PROTOCOL -> ngsMeasurementEntry.runProtocol();
        case INDEX_I7 -> ngsMeasurementEntry.indexI7();
        case INDEX_I5 -> ngsMeasurementEntry.indexI5();
        case COMMENT -> ngsMeasurementEntry.comment();
      };
      var cell = getOrCreateCell(entryRow, measurementColumn.getIndex());
      cell.setCellValue(value);
      cell.setCellStyle(defaultStyle);
      if (measurementColumn.isReadOnly()) {
        cell.setCellStyle(readOnlyCellStyle);
      }
    }


  }

  @Override
  public int numberOfRowsToGenerate() {
    return DEFAULT_GENERATED_ROW_COUNT;
  }

  @Override
  public void enterValuesAsRows(Sheet sheet, CellStyles cellStyles) {
    var rowIndex = 1; //we start in row 1 as row 0 ist the header
    for (NGSMeasurementEntry measurement : measurements) {
      Row row = getOrCreateRow(sheet, rowIndex);
      writeMeasurementIntoRow(measurement, row, cellStyles.defaultCellStyle(),
          cellStyles.readOnlyCellStyle());
      rowIndex++;
    }
  }

  @Override
  public Column[] getColumns() {
    return NGSMeasurementEditColumn.values();
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    MeasurementWorkbookFactory.addValidation(hiddenSheet, sheet, 1, numberOfRowsToGenerate() - 1,
        NGSMeasurementEditColumn.SEQUENCING_READ_TYPE.getIndex(),
        "Sequencing read type", SequencingReadType.getOptions());
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    MeasurementWorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        NGSMeasurementEditColumn.ORGANISATION_URL.getIndex(), "https://ror.org");
    MeasurementWorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        NGSMeasurementEditColumn.INSTRUMENT.getIndex(), "https://rdm.qbic.uni-tuebingen.de");
  }
}
