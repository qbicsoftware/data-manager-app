package life.qbic.datamanager.files.export.measurement;

import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.getOrCreateRow;

import java.util.List;
import life.qbic.datamanager.files.export.measurement.ProteomicsWorkbooks.DigestionMethod;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementEditColumn;
import life.qbic.datamanager.views.projects.project.measurements.ProteomicsMeasurementEntry;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ProteomicsEditFactory implements MeasurementWorkbookFactory {

  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;
  private final List<ProteomicsMeasurementEntry> measurements;

  public ProteomicsEditFactory(List<ProteomicsMeasurementEntry> measurements) {
    this.measurements = measurements;
  }

  @Override
  public int numberOfRowsToGenerate() {
    return DEFAULT_GENERATED_ROW_COUNT;
  }

  @Override
  public void enterValuesAsRows(Sheet sheet, CellStyles cellStyles) {
    var rowIndex = 1;
    for (ProteomicsMeasurementEntry measurement : measurements) {
      var row = getOrCreateRow(sheet, rowIndex);
      createMeasurementEntry(measurement, row, cellStyles.defaultCellStyle(),
          cellStyles.readOnlyCellStyle());
      rowIndex++;
    }
  }

  @Override
  public Column[] getColumns() {
    return ProteomicsMeasurementEditColumn.values();
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    MeasurementWorkbookFactory.addValidation(
        hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        ProteomicsMeasurementEditColumn.DIGESTION_METHOD.getIndex(),
        "Digestion method", DigestionMethod.getOptions()
    );
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    MeasurementWorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        ProteomicsMeasurementEditColumn.ORGANISATION_URL.getIndex(), "https://ror.org");
    MeasurementWorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        ProteomicsMeasurementEditColumn.MS_DEVICE.getIndex(),
        "https://rdm.qbic.uni-tuebingen.de");
  }


  private static void createMeasurementEntry(ProteomicsMeasurementEntry pxpEntry, Row entryRow,
      CellStyle defaultStyle,
      CellStyle readOnlyStyle) {

    for (ProteomicsMeasurementEditColumn measurementColumn : ProteomicsMeasurementEditColumn.values()) {
      var value = switch (measurementColumn) {
        case MEASUREMENT_ID -> pxpEntry.measurementCode();
        case SAMPLE_ID -> pxpEntry.sampleInformation().sampleId();
        case SAMPLE_NAME -> pxpEntry.sampleInformation().sampleName();
        case POOL_GROUP -> pxpEntry.samplePoolGroup();
        case TECHNICAL_REPLICATE_NAME -> pxpEntry.technicalReplicateName();
        case ORGANISATION_URL -> pxpEntry.organisationId();
        case ORGANISATION_NAME -> pxpEntry.organisationName();
        case FACILITY -> pxpEntry.facility();
        case MS_DEVICE -> pxpEntry.msDeviceCURIE();
        case MS_DEVICE_NAME -> pxpEntry.msDeviceName();
        case CYCLE_FRACTION_NAME -> pxpEntry.fractionName();
        case DIGESTION_METHOD -> pxpEntry.digestionMethod();
        case DIGESTION_ENZYME -> pxpEntry.digestionEnzyme();
        case ENRICHMENT_METHOD -> pxpEntry.enrichmentMethod();
        case INJECTION_VOLUME -> pxpEntry.injectionVolume();
        case LC_COLUMN -> pxpEntry.lcColumn();
        case LCMS_METHOD -> pxpEntry.lcmsMethod();
        case LABELING_TYPE -> pxpEntry.labelingType();
        case LABEL -> pxpEntry.label();
        case COMMENT -> pxpEntry.comment();
      };
      var cell = getOrCreateCell(entryRow, measurementColumn.getIndex());
      cell.setCellValue(value);
      cell.setCellStyle(defaultStyle);
      if (measurementColumn.isReadOnly()) {
        cell.setCellStyle(readOnlyStyle);
      }
    }
  }
}
