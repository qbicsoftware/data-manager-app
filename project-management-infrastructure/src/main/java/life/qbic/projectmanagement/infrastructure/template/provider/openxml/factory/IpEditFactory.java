package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.IPMeasurementEditColumn;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.util.XLSXTemplateHelper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class IpEditFactory implements WorkbookFactory {

  private static final int DEFAULT_GENERATED_ROW_COUNT = 2000;
  private final List<MeasurementEntryIP> measurements;

  public IpEditFactory(List<MeasurementEntryIP> measurements) {
    this.measurements = Objects.requireNonNull(measurements);
  }

  public record MeasurementEntryIP(
      String measurementId,
      String sampleId,
      String sampleName,
      String measurementName,
      String cycleFractionName,
      String sampleMass,
      String sampleVolume,
      String prepDate,
      String enrichmentMethod,
      String mhcAntibody,
      String mhcTypingMethod,
      String facility,
      String organisationIRI,
      String organisationName,
      String msRunDate,
      String dataAcquisition,
      String instrumentIRI,
      String instrumentName,
      String lcmsMethod,
      String lcColumn,
      String chargeRange,
      String ionMobilityRange,
      String massRange,
      String retentionTimeRange,
      String comment
  ) {

    public MeasurementEntryIP {
      Objects.requireNonNull(measurementId);
      Objects.requireNonNull(sampleId);
      Objects.requireNonNull(sampleName);
      Objects.requireNonNull(measurementName);
      Objects.requireNonNull(cycleFractionName);
      Objects.requireNonNull(sampleMass);
      Objects.requireNonNull(sampleVolume);
      Objects.requireNonNull(prepDate);
      Objects.requireNonNull(enrichmentMethod);
      Objects.requireNonNull(mhcAntibody);
      Objects.requireNonNull(mhcTypingMethod);
      Objects.requireNonNull(facility);
      Objects.requireNonNull(organisationIRI);
      Objects.requireNonNull(organisationName);
      Objects.requireNonNull(msRunDate);
      Objects.requireNonNull(dataAcquisition);
      Objects.requireNonNull(instrumentIRI);
      Objects.requireNonNull(instrumentName);
      Objects.requireNonNull(lcmsMethod);
      Objects.requireNonNull(lcColumn);
      Objects.requireNonNull(chargeRange);
      Objects.requireNonNull(ionMobilityRange);
      Objects.requireNonNull(massRange);
      Objects.requireNonNull(retentionTimeRange);
      Objects.requireNonNull(comment);
    }
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
    return "Immunopeptidomics Measurement Metadata";
  }

  @Override
  public Column[] getColumns() {
    return IPMeasurementEditColumn.values();
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    // No special validations for IP measurements currently
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    // No specific column needs longest value currently
    return Optional.empty();
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        IPMeasurementEditColumn.ORGANISATION_URL.index(), "https://ror.org");
    WorkbookFactory.convertToHeaderWithLink(header, creationHelper, cellStyles,
        IPMeasurementEditColumn.INSTRUMENT.index(), "https://rdm.qbic.uni-tuebingen.de");
  }

  private static void createMeasurementEntry(MeasurementEntryIP ipEntry, Row entryRow,
      CellStyle defaultStyle,
      CellStyle readOnlyStyle) {

    for (IPMeasurementEditColumn measurementColumn : IPMeasurementEditColumn.values()) {
      var value = switch (measurementColumn) {
        case MEASUREMENT_ID -> ipEntry.measurementId();
        case SAMPLE_ID -> ipEntry.sampleId();
        case SAMPLE_NAME -> ipEntry.sampleName();
        case MEASUREMENT_NAME -> ipEntry.measurementName();
        case CYCLE_FRACTION_NAME -> ipEntry.cycleFractionName();
        case SAMPLE_MASS -> ipEntry.sampleMass();
        case SAMPLE_VOLUME -> ipEntry.sampleVolume();
        case PREP_DATE -> ipEntry.prepDate();
        case ENRICHMENT_METHOD -> ipEntry.enrichmentMethod();
        case MHC_ANTIBODY -> ipEntry.mhcAntibody();
        case MHC_TYPING_METHOD -> ipEntry.mhcTypingMethod();
        case FACILITY -> ipEntry.facility();
        case ORGANISATION_URL -> ipEntry.organisationIRI();
        case ORGANISATION_NAME -> ipEntry.organisationName();
        case MS_RUN_DATE -> ipEntry.msRunDate();
        case DATA_ACQUISITION -> ipEntry.dataAcquisition();
        case INSTRUMENT -> ipEntry.instrumentIRI();
        case INSTRUMENT_NAME -> ipEntry.instrumentName();
        case LCMS_METHOD -> ipEntry.lcmsMethod();
        case LC_COLUMN -> ipEntry.lcColumn();
        case CHARGE_RANGE -> ipEntry.chargeRange();
        case ION_MOBILITY_RANGE -> ipEntry.ionMobilityRange();
        case MASS_RANGE -> ipEntry.massRange();
        case RETENTION_TIME_RANGE -> ipEntry.retentionTimeRange();
        case COMMENT -> ipEntry.comment();
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
