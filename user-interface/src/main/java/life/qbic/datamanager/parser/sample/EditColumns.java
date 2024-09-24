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
public enum EditColumns {
  SAMPLE_ID("QBiC Sample Id", 0, true),
  ANALYSIS("Analysis to be performed", 1, false),
  SAMPLE_NAME("Sample Name", 2, false),
  BIOLOGICAL_REPLICATE("Biological Replicate", 3, false),
  CONDITION("Condition", 4, false),
  SPECIES("Species", 5, false),
  ANALYTE("Analyte", 6, false),
  SPECIMEN("Specimen", 7, false),
  COMMENT("Comment", 8, false);

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;

  public static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(EditColumns::columnIndex)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   * @param readOnly    is the property read only
   */
  EditColumns(String headerName, int columnIndex, boolean readOnly) {
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
