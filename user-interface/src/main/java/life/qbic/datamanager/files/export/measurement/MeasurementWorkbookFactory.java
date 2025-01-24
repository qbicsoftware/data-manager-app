package life.qbic.datamanager.files.export.measurement;

import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.createReadOnlyCellStyle;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.createBoldCellStyle;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.createDefaultCellStyle;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.createLinkHeaderCellStyle;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.createReadOnlyHeaderCellStyle;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.lockSheet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.files.export.WorkbookFactory;
import life.qbic.datamanager.files.export.XLSXTemplateHelper;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.importing.parser.ExampleProvider.Helper;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.lang.NonNull;

public interface MeasurementWorkbookFactory extends WorkbookFactory {


  default Row createHeaderCells(Sheet sheet, CellStyles cellStyles,
      @NonNull Column[] columns) {
    Row header = getOrCreateRow(sheet, 0);
    for (Column column : columns) {
      var cell = getOrCreateCell(header, column.getIndex());
      if (column.isMandatory()) {
        cell.setCellValue(column.getName() + "*");
      } else {
        cell.setCellValue(column.getName());
      }

      //add helper to header
      column.getFillHelp().ifPresent(
          helper -> XLSXTemplateHelper.addInputHelper(sheet,
              column.getIndex(),
              0,
              column.getIndex(),
              0,
              helper.exampleValue(),
              helper.description()));

      cell.setCellStyle(cellStyles.boldStyle());
      if (column.isReadOnly()) {
        cell.setCellStyle(cellStyles.readOnlyHeaderStyle());
      }
    }
    return header;
  }

  int numberOfRowsToGenerate();

  void enterValuesAsRows(Sheet sheet, CellStyles cellStyles);

  @Override
  default Workbook createWorkbook() {
    var workbook = new XSSFWorkbook();
    CreationHelper creationHelper = workbook.getCreationHelper();

    CellStyles cellStyles = createCellStyles(workbook);

    String sheetName = "NGS Measurement Metadata";
    Sheet sheet = workbook.createSheet(sheetName);

    Column[] columns = getColumns();
    Row header = createHeaderCells(sheet, cellStyles, columns);
    customizeHeaderCells(header, creationHelper, cellStyles);
    addPropertyInformation(columns, workbook, cellStyles);

    enterValuesAsRows(sheet, cellStyles);

    Sheet hiddenSheet = workbook.createSheet("hidden");
    customizeValidation(hiddenSheet, sheet);
    lockSheet(hiddenSheet);
    hideSheet(workbook, hiddenSheet);

    XLSXTemplateHelper.setColumnAutoWidth(sheet, 0, columns.length - 1);
    workbook.setActiveSheet(0);
    return workbook;
  }

  Column[] getColumns();

  void customizeValidation(Sheet hiddenSheet, Sheet sheet);

  static void addValidation(Sheet hiddenSheet, Sheet sheet, int startRowIndex, int stopRowIndex,
      int columnIndex, String name, List<String> options) {
    Name namedArea = createOptionArea(hiddenSheet, name, options);

    XLSXTemplateHelper.addDataValidation(sheet,
        columnIndex,
        startRowIndex,
        columnIndex,
        stopRowIndex,
        namedArea);
  }

  default void addPropertyInformation(Column[] columns, XSSFWorkbook workbook,
      CellStyles cellStyles) {
    // add property information order of columns matters!!
    for (Column column : Arrays.stream(columns)
        .sorted(Comparator.comparing(Column::getIndex)).toList()) {
      // add property information
      var exampleValue = column.getFillHelp().map(Helper::exampleValue).orElse("");
      var description = column.getFillHelp().map(Helper::description).orElse("");
      XLSXTemplateHelper.addPropertyInformation(workbook,
          column.getName(),
          column.isMandatory(),
          exampleValue,
          description,
          cellStyles.defaultCellStyle(),
          cellStyles.boldStyle());
    }
  }

  void customizeHeaderCells(Row header, CreationHelper creationHelper, CellStyles cellStyles);

  static void convertToHeaderWithLink(Row header, CreationHelper creationHelper,
      CellStyles cellStyles, int columnIndex, String linkAddress) {
    Cell headerCell = getOrCreateCell(header, columnIndex);
    Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
    hyperlink.setAddress(linkAddress);
    headerCell.setCellStyle(cellStyles.linkHeaderStyle());
    headerCell.setHyperlink(hyperlink);
  }

  default CellStyles createCellStyles(XSSFWorkbook workbook) {
    CellStyle readOnlyCellStyle = createReadOnlyCellStyle(workbook);
    CellStyle readOnlyHeaderStyle = createReadOnlyHeaderCellStyle(workbook);
    CellStyle boldStyle = createBoldCellStyle(workbook);
    CellStyle linkHeaderStyle = createLinkHeaderCellStyle(workbook);
    CellStyle defaultCellStyle = createDefaultCellStyle(workbook);

    return new CellStyles(readOnlyHeaderStyle, linkHeaderStyle, boldStyle, defaultCellStyle,
        readOnlyCellStyle);
  }

  record CellStyles(CellStyle readOnlyHeaderStyle, CellStyle linkHeaderStyle, CellStyle boldStyle,
                    CellStyle defaultCellStyle, CellStyle readOnlyCellStyle) {

  }

}
