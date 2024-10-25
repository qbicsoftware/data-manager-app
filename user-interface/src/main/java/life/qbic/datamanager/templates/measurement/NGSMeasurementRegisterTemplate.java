package life.qbic.datamanager.templates.measurement;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.createBoldCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createDefaultCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createLinkHeaderCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createReadOnlyHeaderCellStyle;
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
import life.qbic.datamanager.parser.measurement.NGSMeasurementRegisterColumn;
import life.qbic.datamanager.templates.Template;
import life.qbic.datamanager.templates.XLSXTemplateHelper;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * <b>NGS Measurement Content Provider</b>
 * <p>
 * Implementation of the {@link DownloadContentProvider} providing the content and file name for any
 * files created from {@link NGSMeasurement} and {@link NGSMeasurementMetadata}
 * </p>
 */
public class NGSMeasurementRegisterTemplate extends Template implements DownloadContentProvider {

  private static final String NGS_MEASUREMENT_TEMPLATE_FILENAME = "ngs_measurement_registration_sheet.xlsx";
  private static final String NGS_MEASUREMENT_TEMPLATE_DOMAIN_NAME = "Genomics Template";
  private static final Logger log = logger(NGSMeasurementRegisterTemplate.class);
  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;

  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= NGSMeasurementRegisterColumn.values().length; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  @Override
  public String getDomainName() {
    return NGS_MEASUREMENT_TEMPLATE_DOMAIN_NAME;
  }

  @Override
  public byte[] getContent() {

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      CellStyle readOnlyHeaderStyle = createReadOnlyHeaderCellStyle(workbook);
      CellStyle boldStyle = createBoldCellStyle(workbook);
      CellStyle linkHeaderStyle = createLinkHeaderCellStyle(workbook);
      CellStyle defaultCellStyle = createDefaultCellStyle(workbook);

      Sheet sheet = workbook.createSheet("NGS Measurement Metadata");

      Row header = getOrCreateRow(sheet, 0);
      for (NGSMeasurementRegisterColumn column : NGSMeasurementRegisterColumn.values()) {
        var cell = getOrCreateCell(header, column.columnIndex());
        if (column.isMandatory()) {
          cell.setCellValue(column.headerName() + "*");
        } else {
          cell.setCellValue(column.headerName());
        }
        cell.setCellStyle(boldStyle);
        if (column.isReadOnly()) {
          cell.setCellStyle(readOnlyHeaderStyle);
        } else if (column.equals(NGSMeasurementRegisterColumn.ORGANISATION_URL)) {
          CreationHelper creationHelper = workbook.getCreationHelper();
          Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
          hyperlink.setAddress("https://ror.org");
          cell.setCellStyle(linkHeaderStyle);
          cell.setHyperlink(hyperlink);
        } else if (column.equals(NGSMeasurementRegisterColumn.INSTRUMENT)) {
          CreationHelper creationHelper = workbook.getCreationHelper();
          Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
          hyperlink.setAddress("https://rdm.qbic.uni-tuebingen.de");
          cell.setCellStyle(linkHeaderStyle);
          cell.setHyperlink(hyperlink);
        }

        //add helper to header
        column.getFillHelp().ifPresent(
            helper -> XLSXTemplateHelper.addInputHelper(sheet,
                column.columnIndex(),
                0,
                column.columnIndex(),
                0,
                helper.exampleValue(),
                helper.description())
        );
      }

      var startIndex = 1; // start in row number 2 with index 1 as the header row has number 1 index 0
      // make sure to create the visible sheet first
      Sheet hiddenSheet = workbook.createSheet("hidden");
      Name sequencingReadTypeArea = createOptionArea(hiddenSheet,
          "Sequencing read type", SequencingReadType.getOptions(), defaultCellStyle);

      XLSXTemplateHelper.addDataValidation(sheet,
          NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.columnIndex(),
          startIndex,
          NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.columnIndex(),
          DEFAULT_GENERATED_ROW_COUNT - 1,
          sequencingReadTypeArea);

      for (NGSMeasurementRegisterColumn column : NGSMeasurementRegisterColumn.values()) {
        column.getFillHelp().ifPresent(
            helper -> XLSXTemplateHelper.addInputHelper(sheet,
                column.columnIndex(),
                startIndex,
                column.columnIndex(),
                DEFAULT_GENERATED_ROW_COUNT - 1,
                helper.exampleValue(),
                helper.description()));
      }

      // add property information order of columns matters!!
      for (NGSMeasurementRegisterColumn column : Arrays.stream(
              NGSMeasurementRegisterColumn.values())
          .sorted(Comparator.comparing(NGSMeasurementRegisterColumn::columnIndex)).toList()) {
        // add property information
        var exampleValue = column.getFillHelp().map(Helper::exampleValue).orElse("");
        var description = column.getFillHelp().map(Helper::description).orElse("");
        XLSXTemplateHelper.addPropertyInformation(workbook,
            column.headerName(),
            column.isMandatory(),
            exampleValue,
            description,
            defaultCellStyle,
            boldStyle);
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
    return NGS_MEASUREMENT_TEMPLATE_FILENAME;
  }

}
