package life.qbic.datamanager.parser.measurement;

import java.util.Arrays;

/**
 * <b>NGS Measurement Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for NGS measurement registration
 * in the context of measurement file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum NGSMeasurementRegisterColumn {

  SAMPLE_ID("QBiC Sample Id", 0, false, true),
  SAMPLE_NAME("Sample Name", 1, false, false),
  POOL_GROUP("Sample Pool Group", 2, false, false),
  ORGANISATION_ID("Organisation ID", 3, false, true),
  FACILITY("Facility", 4, false, true),
  INSTRUMENT("Instrument", 5, false, true),
  SEQUENCING_READ_TYPE("Sequencing Read Type", 6, false, true),
  LIBRARY_KIT("Library Kit", 7, false, false),
  FLOW_CELL("Flow Cell", 8, false, false),
  SEQUENCING_RUN_PROTOCOL("Sequencing Run Protocol", 11, false, false),
  INDEX_I7("Index i7", 9, false, false),
  INDEX_I5("Index i5", 10, false, false),
  COMMENT("Comment", 11, false, false),
  ;

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(NGSMeasurementRegisterColumn::columnIndex)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   * @param readOnly    is the property read only
   * @param mandatory
   */
  NGSMeasurementRegisterColumn(String headerName, int columnIndex, boolean readOnly,
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

  public boolean readOnly() {
    return readOnly;
  }

  public boolean isMandatory() {
    return mandatory;
  }
}
