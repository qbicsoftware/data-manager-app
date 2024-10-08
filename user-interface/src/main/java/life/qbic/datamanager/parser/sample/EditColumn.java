package life.qbic.datamanager.parser.sample;

import java.util.Arrays;

/**
 * <b>Sample Edit Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for sample edit
 * in the context of sample batch file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum EditColumn {
  SAMPLE_ID("QBiC Sample Id", 0, true, true),
  ANALYSIS("Analysis to be performed", 1, false, true),
  SAMPLE_NAME("Sample Name", 2, false, true),
  BIOLOGICAL_REPLICATE("Biological Replicate", 3, false, false),
  CONDITION("Condition", 4, false, true),
  SPECIES("Species", 5, false, true),
  ANALYTE("Analyte", 6, false, true),
  SPECIMEN("Specimen", 7, false, true),
  COMMENT("Comment", 8, false, false);

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  public static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(EditColumn::columnIndex)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   * @param readOnly    is the property read only
   * @param mandatory
   */
  EditColumn(String headerName, int columnIndex, boolean readOnly, boolean mandatory) {
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
