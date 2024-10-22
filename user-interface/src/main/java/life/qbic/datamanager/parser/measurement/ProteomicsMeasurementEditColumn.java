package life.qbic.datamanager.parser.measurement;

import java.util.Optional;
import life.qbic.datamanager.parser.Column;
import life.qbic.datamanager.parser.ExampleProvider.Helper;

/**
 * <b>NGS Measurement Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for NGS measurement registration and edit
 * in the context of measurement file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum ProteomicsMeasurementEditColumn implements Column {

  MEASUREMENT_ID("Measurement ID", 0, true, true),
  SAMPLE_ID("QBiC Sample Id", 1, true, true),
  SAMPLE_NAME(
      "Sample Name", 2, true, false),
  POOL_GROUP("Sample Pool Group", 3, true, false),
  TECHNICAL_REPLICATE_NAME("Technical Replicate", 4, false, false),
  ORGANISATION_ID("Organisation ID", 5, false, true),
  ORGANISATION_NAME("Organisation Name", 6, true, false),
  FACILITY("Facility", 7, false, true),
  MS_DEVICE("MS Device", 8, false, true),
  MS_DEVICE_NAME("MS Device Name", 9, true, true),
  CYCLE_FRACTION_NAME("Cycle/Fraction Name", 10, false, false),
  DIGESTION_METHOD("Digestion Method", 11, false, true),
  DIGESTION_ENZYME("Digestion Enzyme", 12, false, true),
  ENRICHMENT_METHOD("Enrichment Method", 13, false, false),
  INJECTION_VOLUME("Injection Volume (µL)", 14, false, false),
  LC_COLUMN("LC Column", 15, false, true),
  LCMS_METHOD("LCMS Method", 16, false, false),
  LABELING_TYPE("Labeling Type", 17, false, false),
  LABEL("Label", 18, false, false),
  COMMENT("Comment", 19, false, false),
  ;
  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  ProteomicsMeasurementEditColumn(String headerName, int columnIndex, boolean readOnly,
      boolean mandatory) {
    this.headerName = headerName;
    this.columnIndex = columnIndex;
    this.readOnly = readOnly;
    this.mandatory = mandatory;
  }

  public String headerName() {
    return headerName;
  }

  public int columnIndex() {
    return columnIndex;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  @Override
  public Optional<Helper> getFillHelp() {
    return Optional.empty();
  }
}
