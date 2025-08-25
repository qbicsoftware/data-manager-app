package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.ProteomicsMeasurementEditColumn;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.ProteomicsWorkbooks.DigestionMethod;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.util.XLSXTemplateHelper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ProteomicsEditFactory implements WorkbookFactory {

  private static final int DEFAULT_GENERATED_ROW_COUNT = 2000;
  private final List<MeasurementEntryPxP> measurements;

  public ProteomicsEditFactory(List<MeasurementEntryPxP> measurements) {
    this.measurements = Objects.requireNonNull(measurements);
  }

  public record MeasurementEntryPxP(
    String measurementId,
    String sampleId,
    String sampleName,
    String poolGroup,
    String technicalReplicateName,
    String oranisationIRI,
    String organisationName,
    String facility,
    String msDevice,
    String msDeviceName,
    String fractionName,
    String digestionMethod,
    String digestionEnzyme,
    String enrichmentMethod,
    String injectionVolume,
    String lcColumn,
    String lcmsMethod,
    String labelingType,
    String label,
    String comment) {
  }

  @Override
  public int numberOfRowsToGenerate() {
    return DEFAULT_GENERATED_ROW_COUNT;
  }

  @Override
  public void enterValuesAsRows(Sheet sheet, CellStyles cellStyles) {
    var rowIndex = 1;
    for (var measurement : measurements) {
      var row = XLSXTemplateHelper.getOrCreateRow(sheet, rowIndex);
      createMeasurementEntry(measurement, row, cellStyles.defaultCellStyle(),
          cellStyles.readOnlyCellStyle());
      rowIndex++;
    }
  }

  @Override
  public String sheetName() {
    return "Proteomics Measurement Metadata";
  }

  @Override
  public Column[] getColumns() {
    return ProteomicsMeasurementEditColumn.values();
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    WorkbookFactory.addValidation(
        hiddenSheet,
        sheet,
        1,
        numberOfRowsToGenerate() - 1,
        ProteomicsMeasurementEditColumn.DIGESTION_METHOD.index(),
        "Digestion method", DigestionMethod.getOptions()
    );
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (ProteomicsMeasurementEditColumn.DIGESTION_METHOD.index() == columnIndex) {
      return DigestionMethod.getOptions().stream().reduce(keepLongerString);
    }
    return Optional.empty();
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        ProteomicsMeasurementEditColumn.ORGANISATION_URL.index(), "https://ror.org");
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        ProteomicsMeasurementEditColumn.MS_DEVICE.index(),
        "https://rdm.qbic.uni-tuebingen.de");
  }


  private static void createMeasurementEntry(MeasurementEntryPxP pxpEntry, Row entryRow,
      CellStyle defaultStyle,
      CellStyle readOnlyStyle) {

    for (ProteomicsMeasurementEditColumn measurementColumn : ProteomicsMeasurementEditColumn.values()) {
      var value = switch (measurementColumn) {
        case ProteomicsMeasurementEditColumn.MEASUREMENT_ID -> pxpEntry.measurementId();
        case ProteomicsMeasurementEditColumn.SAMPLE_ID -> pxpEntry.sampleId();
        case ProteomicsMeasurementEditColumn.SAMPLE_NAME -> pxpEntry.sampleName();
        case ProteomicsMeasurementEditColumn.POOL_GROUP -> pxpEntry.poolGroup();
        case ProteomicsMeasurementEditColumn.TECHNICAL_REPLICATE_NAME -> pxpEntry.technicalReplicateName();
        case ProteomicsMeasurementEditColumn.ORGANISATION_URL -> pxpEntry.oranisationIRI();
        case ProteomicsMeasurementEditColumn.ORGANISATION_NAME -> pxpEntry.organisationName();
        case ProteomicsMeasurementEditColumn.FACILITY -> pxpEntry.facility();
        case ProteomicsMeasurementEditColumn.MS_DEVICE -> pxpEntry.msDevice();
        case ProteomicsMeasurementEditColumn.MS_DEVICE_NAME -> pxpEntry.msDeviceName();
        case ProteomicsMeasurementEditColumn.CYCLE_FRACTION_NAME -> pxpEntry.fractionName();
        case ProteomicsMeasurementEditColumn.DIGESTION_METHOD -> pxpEntry.digestionMethod();
        case ProteomicsMeasurementEditColumn.DIGESTION_ENZYME -> pxpEntry.digestionEnzyme();
        case ProteomicsMeasurementEditColumn.ENRICHMENT_METHOD -> pxpEntry.enrichmentMethod();
        case ProteomicsMeasurementEditColumn.INJECTION_VOLUME -> pxpEntry.injectionVolume();
        case ProteomicsMeasurementEditColumn.LC_COLUMN -> pxpEntry.lcColumn();
        case ProteomicsMeasurementEditColumn.LCMS_METHOD -> pxpEntry.lcmsMethod();
        case ProteomicsMeasurementEditColumn.LABELING_TYPE -> pxpEntry.labelingType();
        case ProteomicsMeasurementEditColumn.LABEL -> pxpEntry.label();
        case ProteomicsMeasurementEditColumn.COMMENT -> pxpEntry.comment();
      };
      var cell = XLSXTemplateHelper.getOrCreateCell(entryRow, measurementColumn.index());
      cell.setCellValue(value);
      cell.setCellStyle(defaultStyle);
      if (measurementColumn.isReadOnly()) {
        cell.setCellStyle(readOnlyStyle);
      }
    }
  }
}
