package life.qbic.datamanager.parser.sample;

import java.util.Arrays;
import java.util.Optional;
import life.qbic.datamanager.parser.Column;
import life.qbic.datamanager.parser.ExampleProvider.Helper;

/**
 * <b>Sample Register Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for sample registration
 * in the context of sample batch file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum RegisterColumn implements Column {

  SAMPLE_NAME("Sample Name", 0, false, true),
  ANALYSIS("Analysis to be performed", 1, false, true),
  BIOLOGICAL_REPLICATE("Biological Replicate", 2, false, false),
  CONDITION("Condition", 3, false, true),
  SPECIES("Species", 4, false, true),
  ANALYTE("Analyte", 5, false, true),
  SPECIMEN("Specimen", 6, false, true),
  COMMENT("Comment", 7, false, false);

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  public static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(RegisterColumn::columnIndex)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   * @param readOnly    is the property read only
   * @param mandatory
   */
  RegisterColumn(String headerName, int columnIndex, boolean readOnly, boolean mandatory) {
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
