package life.qbic.datamanager.parser.sample;

import java.util.Arrays;

/**
 * <b>Sample Register Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for sample registration
 * in the context of sample batch file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum RegisterColumns {

  ANALYSIS("Analysis to be performed", 0, false),
  SAMPLE_NAME("Sample Name", 1, false),
  BIOLOGICAL_REPLICATE("Biological Replicate", 2, false),
  CONDITION("Condition", 3, false),
  SPECIES("Species", 4, false),
  ANALYTE("Analyte", 5, false),
  SPECIMEN("Specimen", 6, false),
  COMMENT("Comment", 7, false);

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;

  public static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(RegisterColumns::columnIndex)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   * @param readOnly    is the property read only
   */
  RegisterColumns(String headerName, int columnIndex, boolean readOnly) {
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
