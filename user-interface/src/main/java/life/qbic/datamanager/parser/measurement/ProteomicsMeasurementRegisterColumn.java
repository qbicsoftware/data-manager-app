package life.qbic.datamanager.parser.measurement;

import java.util.Optional;
import life.qbic.datamanager.parser.Column;
import life.qbic.datamanager.parser.ExampleProvider.Helper;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public enum ProteomicsMeasurementRegisterColumn implements Column {

  SAMPLE_ID("QBiC Sample Id", 0, true, true),
  SAMPLE_NAME(
      "Sample Name", 1, true, false),
  POOL_GROUP("Sample Pool Group", 2, true, false),
  TECHNICAL_REPLICATE_NAME("Technical Replicate", 3, false, false),
  CYCLE_FRACTION_NAME("Cycle/Fraction Name", 4, false, false),
  ORGANISATION_ID("Organisation ID", 5, false, true),
  FACILITY("Facility", 6, false, true),
  LC_COLUMN("LC Column", 7, false, true),
  MS_DEVICE("MS Device", 8, false, true),
  LCMS_METHOD("LCMS Method", 9, false, false),
  DIGESTION_METHOD("Digestion Method", 10, false, true),
  DIGESTION_ENZYME("Digestion Enzyme", 11, false, true),
  ENRICHMENT_METHOD("Enrichment Method", 12, false, false),
  LABELING_TYPE("Labeling Type", 13, false, false),
  LABEL("Label", 14, false, false),
  INJECTION_VOLUME("Injection Volume (µL)", 15, false, false),
  COMMENT("Comment", 16, false, false),
  ;
  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  ProteomicsMeasurementRegisterColumn(String headerName, int columnIndex, boolean readOnly,
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
