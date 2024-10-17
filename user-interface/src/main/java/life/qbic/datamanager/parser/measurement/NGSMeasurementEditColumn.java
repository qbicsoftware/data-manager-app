package life.qbic.datamanager.parser.measurement;

import java.util.Arrays;

/**
 * <b>NGS Measurement Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for NGS measurement edit
 * in the context of measurement file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum NGSMeasurementEditColumn {

  MEASUREMENT_ID("Measurement ID", 0, true, true),
  SAMPLE_ID("QBiC Sample Id", 1, true, true),
  SAMPLE_NAME("Sample Name", 2, true, false),
  POOL_GROUP("Sample Pool Group", 3, true, false),
  ORGANISATION_ID("Organisation ID", 4, false, true),
  ORGANISATION_NAME("Organisation Name", 5, true, false),
  FACILITY("Facility", 6, false, true),
  INSTRUMENT("Instrument", 7, false, true),
  INSTRUMENT_NAME("Instrument Name", 8, true, false),
  SEQUENCING_READ_TYPE("Sequencing Read Type", 9, false, true),
  LIBRARY_KIT("Library Kit", 10, false, false),
  FLOW_CELL("Flow Cell", 11, false, false),
  SEQUENCING_RUN_PROTOCOL("Sequencing Run Protocol", 12, false, false),
  INDEX_I7("Index i7", 13, false, false),
  INDEX_I5("Index i5", 14, false, false),
  COMMENT("Comment", 15, false, false),
  ;

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(NGSMeasurementEditColumn::columnIndex)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   * @param readOnly    is the property read only
   * @param mandatory
   */
  NGSMeasurementEditColumn(String headerName, int columnIndex, boolean readOnly,
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

}
