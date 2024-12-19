package life.qbic.datamanager.templates.measurement;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.addDataValidation;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createBoldCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createDefaultCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createLinkHeaderCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createReadOnlyCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.lockSheet;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.download.DownloadContentProvider;
import life.qbic.datamanager.parser.ExampleProvider.Helper;
import life.qbic.datamanager.parser.measurement.ProteomicsMeasurementEditColumn;
import life.qbic.datamanager.templates.XLSXTemplateHelper;
import life.qbic.datamanager.views.projects.project.measurements.ProteomicsMeasurementEntry;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementProteomicsValidator.DigestionMethod;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** <b>Proteomics Measurement Content Provider</b>
 * <p>
 * Implementation of the {@link DownloadContentProvider} providing the content and file name for any files created
 * from {@link ProteomicsMeasurement}
 * and {@link ProteomicsMeasurementMetadata}
 * </p>
 */
public class ProteomicsMeasurementEditTemplate implements DownloadContentProvider {

  private static final String FILE_NAME_SUFFIX = "proteomics_measurements.xlsx";
  private static final Logger log = logger(ProteomicsMeasurementEditTemplate.class);
  private final List<ProteomicsMeasurementEntry> measurements = new LinkedList<>();
  private static final String DEFAULT_FILE_NAME_PREFIX = "QBiC";
  private String fileNamePrefix = DEFAULT_FILE_NAME_PREFIX;
  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;


  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= 18; col++) {
      sheet.autoSizeColumn(col);
    }
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
      var cell = getOrCreateCell(entryRow, measurementColumn.columnIndex());
      cell.setCellValue(value);
      cell.setCellStyle(defaultStyle);
      if (measurementColumn.isReadOnly()) {
        cell.setCellStyle(readOnlyStyle);
      }
    }
  }

  public void setMeasurements(List<ProteomicsMeasurementEntry> measurements, String fileNamePrefix) {
    this.measurements.clear();
    this.measurements.addAll(measurements);
    this.fileNamePrefix = fileNamePrefix;
  }

  @Override
  public byte[] getContent() {
    if (measurements.isEmpty()) {
      return new byte[0];
    }

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();) {

      CellStyle readOnlyHeaderStyle = XLSXTemplateHelper.createReadOnlyHeaderCellStyle(workbook);
      CellStyle boldStyle = createBoldCellStyle(workbook);
      CellStyle readOnlyStyle = createReadOnlyCellStyle(workbook);
      CellStyle linkHeaderStyle = createLinkHeaderCellStyle(workbook);
      CellStyle defaultStyle = createDefaultCellStyle(workbook);

      Sheet sheet = workbook.createSheet("Proteomics Measurement Metadata");
      Row header = getOrCreateRow(sheet, 0);
      for (ProteomicsMeasurementEditColumn measurementColumn : ProteomicsMeasurementEditColumn.values()) {
        var cell = getOrCreateCell(header, measurementColumn.columnIndex());
        if (measurementColumn.isMandatory()) {
          cell.setCellValue(measurementColumn.headerName() + "*");
        } else {
          cell.setCellValue(measurementColumn.headerName());
        }
        cell.setCellStyle(boldStyle);
        if (measurementColumn.isReadOnly()) {
          cell.setCellStyle(readOnlyHeaderStyle);
        } else if (measurementColumn.equals(ProteomicsMeasurementEditColumn.ORGANISATION_URL)) {
          CreationHelper creationHelper = workbook.getCreationHelper();
          Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
          hyperlink.setAddress("https://ror.org");
          cell.setCellStyle(linkHeaderStyle);
          cell.setHyperlink(hyperlink);
        } else if (measurementColumn.equals(ProteomicsMeasurementEditColumn.MS_DEVICE)) {
          CreationHelper creationHelper = workbook.getCreationHelper();
          Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
          hyperlink.setAddress("https://rdm.qbic.uni-tuebingen.de");
          cell.setCellStyle(linkHeaderStyle);
          cell.setHyperlink(hyperlink);
        }
        //add helper to header
        measurementColumn.getFillHelp().ifPresent(
            helper -> XLSXTemplateHelper.addInputHelper(sheet,
                measurementColumn.columnIndex(),
                0,
                measurementColumn.columnIndex(),
                0,
                helper.exampleValue(),
                helper.description()));
      }

      // add property information order of columns matters!!
      for (ProteomicsMeasurementEditColumn column : Arrays.stream(
              ProteomicsMeasurementEditColumn.values())
          .sorted(Comparator.comparing(ProteomicsMeasurementEditColumn::columnIndex)).toList()) {
        // add property information
        var exampleValue = column.getFillHelp().map(Helper::exampleValue).orElse("");
        var description = column.getFillHelp().map(Helper::description).orElse("");
        XLSXTemplateHelper.addPropertyInformation(workbook,
            column.headerName(),
            column.isMandatory(),
            exampleValue,
            description,
            defaultStyle,
            boldStyle);
      }

      var startIndex = 1; // start in row number 2 with index 1 skipping the header in the first row
      var helperStopIndex = 1; //stop in row number 2 with index 1 as the header row has number 1 index 0
      var rowIndex = startIndex;

      for (ProteomicsMeasurementEntry pxpEntry : measurements) {
        Row entry = getOrCreateRow(sheet, rowIndex);
        createMeasurementEntry(pxpEntry, entry, defaultStyle, readOnlyStyle);
        rowIndex++;
      }
      var generatedRowCount = rowIndex - startIndex;
      assert generatedRowCount == measurements.size() : "all measurements have a corresponding row";

      // make sure to create the visible sheet first
      Sheet hiddenSheet = workbook.createSheet("hidden");
      Name digestionMethodArea = createOptionArea(hiddenSheet, "Digestion Method",
          DigestionMethod.getOptions());

      addDataValidation(sheet,
          ProteomicsMeasurementEditColumn.DIGESTION_METHOD.columnIndex(), startIndex,
          ProteomicsMeasurementEditColumn.DIGESTION_METHOD.columnIndex(),
          DEFAULT_GENERATED_ROW_COUNT - 1,
          digestionMethodArea);

      for (ProteomicsMeasurementEditColumn column : ProteomicsMeasurementEditColumn.values()) {
        column.getFillHelp().ifPresent(
            helper -> XLSXTemplateHelper.addInputHelper(sheet,
                column.columnIndex(),
                startIndex,
                column.columnIndex(),
                helperStopIndex,
                helper.exampleValue(),
                helper.description())
        );
      }

      setAutoWidth(sheet);
      workbook.setActiveSheet(0);

      lockSheet(hiddenSheet);
      hideSheet(workbook, hiddenSheet);

      workbook.write(byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new ApplicationException(ErrorCode.GENERAL, null);
    }
  }


  @Override
  public String getFileName() {
    return String.join("_", fileNamePrefix, FILE_NAME_SUFFIX);
  }
}
