package life.qbic.datamanager.views.projects.project.measurements.download;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;
import life.qbic.datamanager.views.projects.project.measurements.ProteomicsMeasurementEntry;
import life.qbic.logging.api.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** <b>Proteomics Measurement Content Provider</b>
 * <p>
 * Implementation of the {@link DownloadContentProvider} providing the content and file name for any files created
 * from {@link life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement}
 * and {@link life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata}
 * </p>
 */
public class ProteomicsMeasurementContentProvider implements DownloadContentProvider {

  private static final String FILE_NAME_SUFFIX = "proteomics_measurements.xlsx";
  private static final Logger log = logger(ProteomicsMeasurementContentProvider.class);
  private static final byte[] DARK_GREY = {119, 119, 119};
  private static final byte[] LIGHT_GREY = {(byte) 220, (byte) 220, (byte) 220};
  private final List<ProteomicsMeasurementEntry> measurements = new LinkedList<>();
  private static final String DEFAULT_FILE_NAME_PREFIX = "QBiC";
  private String fileNamePrefix = DEFAULT_FILE_NAME_PREFIX;

  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= 18; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  private static void formatHeader(Row header, CellStyle readOnlyHeader, CellStyle boldStyle) {
    var h1 = header.createCell(0);
    h1.setCellValue("Measurement ID");
    h1.setCellStyle(readOnlyHeader);

    var h2 = header.createCell(1);
    h2.setCellValue("QBiC Sample ID");
    h2.setCellStyle(readOnlyHeader);

    var h3 = header.createCell(2);
    h3.setCellValue("Sample Name");
    h3.setCellStyle(readOnlyHeader);

    var h4 = header.createCell(3);
    h4.setCellValue("Sample Pool Group");
    h4.setCellStyle(readOnlyHeader);

    var h5 = header.createCell(4);
    h5.setCellValue("Organisation ID");
    h5.setCellStyle(boldStyle);

    var h6 = header.createCell(5);
    h6.setCellValue("Organisation Name");
    h6.setCellStyle(readOnlyHeader);

    var h7 = header.createCell(6);
    h7.setCellValue("Facility");
    h7.setCellStyle(boldStyle);

    var h8 = header.createCell(7);
    h8.setCellValue("Instrument");
    h8.setCellStyle(boldStyle);

    var h9 = header.createCell(8);
    h9.setCellValue("Instrument Name");
    h9.setCellStyle(readOnlyHeader);

    header.createCell(9).setCellValue("Cycle/Fraction Name");
    header.createCell(10).setCellValue("Digestion Method");
    header.createCell(11).setCellValue("Digestion Enzyme");
    header.createCell(12).setCellValue("Enrichment Method");
    header.createCell(13).setCellValue("Injection Volume (uL)");
    header.createCell(14).setCellValue("LC Column");
    header.createCell(15).setCellValue("LCMS Method");
    header.createCell(16).setCellValue("Labeling Type");
    header.createCell(17).setCellValue("Label");
    header.createCell(18).setCellValue("Comment");

    for (int i = 9; i < 19; i++) {
      header.getCell(i).setCellStyle(boldStyle);
    }
  }

  private static void createMeasurementEntry(ProteomicsMeasurementEntry pxpEntry, Row entry,
      CellStyle readOnlyStyle) {
    var measureCol = entry.createCell(0);
    measureCol.setCellValue(pxpEntry.measurementCode());
    measureCol.setCellStyle(readOnlyStyle);

    var sampleIdCol = entry.createCell(1);
    sampleIdCol.setCellValue(pxpEntry.sampleInformation().sampleId());
    sampleIdCol.setCellStyle(readOnlyStyle);

    var sampleNameCol = entry.createCell(2);
    sampleNameCol.setCellValue(pxpEntry.sampleInformation().sampleName());
    sampleNameCol.setCellStyle(readOnlyStyle);

    var samplePoolCol = entry.createCell(3);
    samplePoolCol.setCellValue(pxpEntry.samplePoolGroup());
    samplePoolCol.setCellStyle(readOnlyStyle);

    entry.createCell(4).setCellValue(pxpEntry.organisationId());

    var organisationNameCol = entry.createCell(5);
    organisationNameCol.setCellValue(pxpEntry.organisationName());
    organisationNameCol.setCellStyle(readOnlyStyle);

    entry.createCell(6).setCellValue(pxpEntry.facility());
    entry.createCell(7).setCellValue(pxpEntry.instrumentCURI());

    var instumentNameCol = entry.createCell(8);
    instumentNameCol.setCellValue(pxpEntry.instrumentName());
    instumentNameCol.setCellStyle(readOnlyStyle);

    entry.createCell(9).setCellValue(pxpEntry.fractionName());
    entry.createCell(10).setCellValue(pxpEntry.digestionMethod());
    entry.createCell(11).setCellValue(pxpEntry.digestionEnzyme());
    entry.createCell(12).setCellValue(pxpEntry.enrichmentMethod());
    entry.createCell(13).setCellValue(Integer.parseInt(pxpEntry.injectionVolume()));
    entry.createCell(14).setCellValue(pxpEntry.lcColumn());
    entry.createCell(15).setCellValue(pxpEntry.lcmsMethod());
    entry.createCell(16).setCellValue(pxpEntry.labelingType());
    entry.createCell(17).setCellValue(pxpEntry.label());
    entry.createCell(18).setCellValue(pxpEntry.comment());
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

    ByteArrayOutputStream byteArrayOutputStream;

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Proteomics Measurement Metadata");

      Row header = sheet.createRow(0);

      CellStyle readOnlyHeader = workbook.createCellStyle();
      readOnlyHeader.setFillForegroundColor(
          new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
      readOnlyHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      XSSFFont fontHeader = (XSSFFont) workbook.createFont();
      fontHeader.setBold(true);
      fontHeader.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
      readOnlyHeader.setFont(fontHeader);

      CellStyle boldStyle = workbook.createCellStyle();
      Font fontBold = workbook.createFont();
      fontBold.setBold(true);
      boldStyle.setFont(fontBold);

      formatHeader(header, readOnlyHeader, boldStyle);

      CellStyle readOnlyStyle = workbook.createCellStyle();
      readOnlyStyle.setFillForegroundColor(new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
      readOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      XSSFFont font = (XSSFFont) workbook.createFont();
      font.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
      readOnlyStyle.setFont(font);

      int rowCounter = 1;

      for (ProteomicsMeasurementEntry pxpEntry : measurements) {
        Row entry = sheet.createRow(rowCounter);
        createMeasurementEntry(pxpEntry, entry, readOnlyStyle);
        rowCounter++;
      }

      setAutoWidth(sheet);

      byteArrayOutputStream = new ByteArrayOutputStream();
      workbook.write(byteArrayOutputStream);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new ApplicationException(ErrorCode.GENERAL, null);
    }

    return byteArrayOutputStream.toByteArray();
  }

  @Override
  public String getFileName() {
    return String.join("_", fileNamePrefix, FILE_NAME_SUFFIX);
  }
}
