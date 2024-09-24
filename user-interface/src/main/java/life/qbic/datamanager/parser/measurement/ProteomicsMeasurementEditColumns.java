package life.qbic.datamanager.parser.measurement;

/**
 * <b>NGS Measurement Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for NGS measurement registration and edit
 * in the context of measurement file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum ProteomicsMeasurementEditColumns {

  MEASUREMENT_ID("Measurement ID", 0, true),
  SAMPLE_ID("QBiC Sample Id", 1, true),
  SAMPLE_NAME(
      "Sample Name", 2, true),
  POOL_GROUP("Sample Pool Group", 3, true),
  TECHNICAL_REPLICATE_NAME("Technical Replicate", 4, false),
  ORGANISATION_ID("Organisation ID", 5, false),
  ORGANISATION_NAME("Organisation Name", 6, true),
  FACILITY("Facility", 7, false),
  MS_DEVICE("MS Device", 8, false),
  MS_DEVICE_NAME("MS Device Name", 9, true),
  CYCLE_FRACTION_NAME("Cycle/Fraction Name", 10, false),
  DIGESTION_METHOD("Digestion Method", 11, false),
  DIGESTION_ENZYME("Digestion Enzyme", 12, false),
  ENRICHMENT_METHOD("Enrichment Method", 13, false),
  INJECTION_VOLUME("Injection Volume (µL)", 14, false),
  LC_COLUMN("LC Column", 15, false),
  LCMS_METHOD("LCMS Method", 16, false),
  LABELING_TYPE("Labeling Type", 17, false),
  LABEL("Label", 18, false),
  COMMENT("Comment", 19, false),
  ;
  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;

  ProteomicsMeasurementEditColumns(String headerName, int columnIndex, boolean readOnly) {
    this.headerName = headerName;
    this.columnIndex = columnIndex;
    this.readOnly = readOnly;
  }

  public String headerName() {
    return headerName;
  }

  public int columnIndex() {
    return columnIndex;
  }

  public boolean readOnly() {
    return readOnly;
  }
}
