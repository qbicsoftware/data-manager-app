package life.qbic.datamanager.templates.measurement;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.addDataValidation;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createBoldCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createDefaultCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createLinkHeaderCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.lockSheet;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.download.DownloadContentProvider;
import life.qbic.datamanager.parser.ExampleProvider.Helper;
import life.qbic.datamanager.parser.measurement.ProteomicsMeasurementRegisterColumn;
import life.qbic.datamanager.templates.Template;
import life.qbic.datamanager.templates.XLSXTemplateHelper;
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
public class ProteomicsMeasurementRegisterTemplate extends Template {

  private static final String MS_MEASUREMENT_TEMPLATE_FILENAME = "proteomics_measurement_registration_sheet.xlsx";
  private static final String MS_MEASUREMENT_TEMPLATE_DOMAIN_NAME = "Proteomics Template";


  private static final Logger log = logger(ProteomicsMeasurementRegisterTemplate.class);
  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;


  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= 18; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  @Override
  public byte[] getContent() {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

      CellStyle readOnlyHeaderStyle = XLSXTemplateHelper.createReadOnlyHeaderCellStyle(workbook);
      CellStyle boldStyle = createBoldCellStyle(workbook);
      CellStyle linkHeaderStyle = createLinkHeaderCellStyle(workbook);
      CellStyle defaultStyle = createDefaultCellStyle(workbook);

      Sheet sheet = workbook.createSheet("Proteomics Measurement Metadata");
      Row header = getOrCreateRow(sheet, 0);
      for (ProteomicsMeasurementRegisterColumn measurementColumn : ProteomicsMeasurementRegisterColumn.values()) {
        var cell = getOrCreateCell(header, measurementColumn.columnIndex());
        if (measurementColumn.isMandatory()) {
          cell.setCellValue(measurementColumn.headerName() + "*");
        } else {
          cell.setCellValue(measurementColumn.headerName());
        }
        cell.setCellStyle(boldStyle);
        if (measurementColumn.isReadOnly()) {
          cell.setCellStyle(readOnlyHeaderStyle);
        } else if (measurementColumn.equals(ProteomicsMeasurementRegisterColumn.ORGANISATION_URL)) {
          CreationHelper creationHelper = workbook.getCreationHelper();
          Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
          hyperlink.setAddress("https://ror.org");
          cell.setCellStyle(linkHeaderStyle);
          cell.setHyperlink(hyperlink);
        } else if (measurementColumn.equals(ProteomicsMeasurementRegisterColumn.MS_DEVICE)) {
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
      for (ProteomicsMeasurementRegisterColumn column : Arrays.stream(
              ProteomicsMeasurementRegisterColumn.values())
          .sorted(Comparator.comparing(ProteomicsMeasurementRegisterColumn::columnIndex))
          .toList()) {
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

      var startIndex = 1; //start in the second row with index 1.
      // make sure to create the visible sheet first
      Sheet hiddenSheet = workbook.createSheet("hidden");
      Name digestionMethodArea = createOptionArea(hiddenSheet, "Digestion Method",
          DigestionMethod.getOptions());

      addDataValidation(sheet,
          ProteomicsMeasurementRegisterColumn.DIGESTION_METHOD.columnIndex(), startIndex,
          ProteomicsMeasurementRegisterColumn.DIGESTION_METHOD.columnIndex(),
          DEFAULT_GENERATED_ROW_COUNT - 1,
          digestionMethodArea);

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
    return MS_MEASUREMENT_TEMPLATE_FILENAME;
  }

  @Override
  public String getDomainName() {
    return MS_MEASUREMENT_TEMPLATE_DOMAIN_NAME;
  }
}
