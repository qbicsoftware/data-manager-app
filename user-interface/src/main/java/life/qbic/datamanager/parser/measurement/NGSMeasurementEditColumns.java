package life.qbic.datamanager.parser.measurement;

import java.util.Arrays;

/**
 * <b>NGS Measurement Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for NGS measurement registration and edit
 * in the context of measurement file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum NGSMeasurementEditColumns {

  MEASUREMENT_ID("Measurement ID", 0, true),
  SAMPLE_ID("QBiC Sample Id", 1, true),
  SAMPLE_NAME("Sample Name", 2, true),
  POOL_GROUP("Sample Pool Group", 3, true),
  ORGANISATION_ID("Organisation ID", 4, false),
  ORGANISATION_NAME("Organisation Name", 5, true),
  FACILITY("Facility", 6, false),
  INSTRUMENT("Instrument", 7, false),
  INSTRUMENT_NAME("Instrument Name", 8, true),
  SEQUENCING_READ_TYPE("Sequencing Read Type", 9, false),
  LIBRARY_KIT("Library Kit", 10, false),
  FLOW_CELL("Flow Cell", 11, false),
  SEQUENCING_RUN_PROTOCOL("Sequencing Run Protocol", 12, false),
  INDEX_I7("Index i7", 13, false),
  INDEX_I5("Index i5", 14, false),
  COMMENT("Comment", 15, false),
  ;

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;

  static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(NGSMeasurementEditColumns::columnIndex)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   * @param readOnly    is the property read only
   */
  NGSMeasurementEditColumns(String headerName, int columnIndex, boolean readOnly) {
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
