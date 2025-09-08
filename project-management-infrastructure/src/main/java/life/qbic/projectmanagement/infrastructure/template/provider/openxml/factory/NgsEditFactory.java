package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.NGSMeasurementEditColumn;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory.NGSWorkbooks.SequencingReadType;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.util.XLSXTemplateHelper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class NgsEditFactory implements WorkbookFactory {

  private static final int DEFAULT_GENERATED_ROW_COUNT = 2000;
  private final List<MeasurementEntryNGS> measurements;

  public NgsEditFactory(List<MeasurementEntryNGS> measurements) {
    this.measurements = Objects.requireNonNull(measurements);
  }

  public record MeasurementEntryNGS(
      String measurementId,
      String sampleId,
      String sampleName,
      String poolGroup,
      String organisationIRI,
      String organisationName,
      String facility,
      String instrumentIRI,
      String instrumentName,
      String sequencingReadType,
      String libraryKit,
      String flowCell,
      String runProtocol,
      String indexI7,
      String indexI5,
      String comment,
      String measurementName
  ) {
    public MeasurementEntryNGS {
      Objects.requireNonNull(measurementId);
      Objects.requireNonNull(sampleId);
      Objects.requireNonNull(sampleName);
      Objects.requireNonNull(poolGroup);
      Objects.requireNonNull(organisationIRI);
      Objects.requireNonNull(organisationName);
      Objects.requireNonNull(facility);
      Objects.requireNonNull(instrumentIRI);
      Objects.requireNonNull(instrumentName);
      Objects.requireNonNull(sequencingReadType);
      Objects.requireNonNull(libraryKit);
      Objects.requireNonNull(flowCell);
      Objects.requireNonNull(runProtocol);
      Objects.requireNonNull(indexI7);
      Objects.requireNonNull(indexI5);
      Objects.requireNonNull(comment);
    }
  }

  private static void writeMeasurementIntoRow(MeasurementEntryNGS ngsMeasurementEntry,
      Row entryRow, CellStyle defaultStyle, CellStyle readOnlyCellStyle) {
    for (NGSMeasurementEditColumn measurementColumn : NGSMeasurementEditColumn.values()) {
      var value = switch (measurementColumn) {
        case NGSMeasurementEditColumn.MEASUREMENT_ID -> ngsMeasurementEntry.measurementId();
        case NGSMeasurementEditColumn.SAMPLE_ID -> ngsMeasurementEntry.sampleId();
        case NGSMeasurementEditColumn.SAMPLE_NAME -> ngsMeasurementEntry.sampleName();
        case NGSMeasurementEditColumn.POOL_GROUP -> ngsMeasurementEntry.poolGroup();
        case NGSMeasurementEditColumn.ORGANISATION_URL -> ngsMeasurementEntry.organisationIRI();
        case NGSMeasurementEditColumn.ORGANISATION_NAME -> ngsMeasurementEntry.organisationName();
        case NGSMeasurementEditColumn.FACILITY -> ngsMeasurementEntry.facility();
        case NGSMeasurementEditColumn.INSTRUMENT -> ngsMeasurementEntry.instrumentIRI();
        case NGSMeasurementEditColumn.INSTRUMENT_NAME -> ngsMeasurementEntry.instrumentName();
        case NGSMeasurementEditColumn.SEQUENCING_READ_TYPE -> ngsMeasurementEntry.sequencingReadType();
        case NGSMeasurementEditColumn.LIBRARY_KIT -> ngsMeasurementEntry.libraryKit();
        case NGSMeasurementEditColumn.FLOW_CELL -> ngsMeasurementEntry.flowCell();
        case NGSMeasurementEditColumn.SEQUENCING_RUN_PROTOCOL -> ngsMeasurementEntry.runProtocol();
        case NGSMeasurementEditColumn.INDEX_I7 -> ngsMeasurementEntry.indexI7();
        case NGSMeasurementEditColumn.INDEX_I5 -> ngsMeasurementEntry.indexI5();
        case NGSMeasurementEditColumn.COMMENT -> ngsMeasurementEntry.comment();
        case NGSMeasurementEditColumn.MEASUREMENT_NAME -> ngsMeasurementEntry.measurementName();
      };
      var cell = XLSXTemplateHelper.getOrCreateCell(entryRow, measurementColumn.index());
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
    for (var measurement : measurements) {
      Row row = XLSXTemplateHelper.getOrCreateRow(sheet, rowIndex);
      writeMeasurementIntoRow(measurement, row, cellStyles.defaultCellStyle(),
          cellStyles.readOnlyCellStyle());
      rowIndex++;
    }
  }

  @Override
  public String sheetName() {
    return "NGS Measurement Metadata";
  }

  @Override
  public Column[] getColumns() {
    return NGSMeasurementEditColumn.values();
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    WorkbookFactory.addValidation(hiddenSheet, sheet, 1, numberOfRowsToGenerate() - 1,
        NGSMeasurementEditColumn.SEQUENCING_READ_TYPE.index(),
        "Sequencing read type", SequencingReadType.getOptions());
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (NGSMeasurementEditColumn.SEQUENCING_READ_TYPE.index() == columnIndex) {
      return SequencingReadType.getOptions().stream().reduce(keepLongerString);
    }
    return Optional.empty();
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        NGSMeasurementEditColumn.ORGANISATION_URL.index(), "https://ror.org");
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        NGSMeasurementEditColumn.INSTRUMENT.index(), "https://rdm.qbic.uni-tuebingen.de");
  }
}
