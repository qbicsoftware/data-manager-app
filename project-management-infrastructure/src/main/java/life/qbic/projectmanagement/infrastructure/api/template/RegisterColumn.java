package life.qbic.projectmanagement.infrastructure.api.template;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.projectmanagement.infrastructure.api.template.ExampleProvider.Helper;

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
  SPECIMEN("Specimen", 5, false, true),
  ANALYTE("Analyte", 6, false, true),
  COMMENT("Comment", 7, false, false);

  private static final ExampleProvider exampleProvider = column -> {
    if (column instanceof RegisterColumn registerColumn) {
      return switch (registerColumn) {
        case SAMPLE_NAME -> new Helper("Free text, e.g. RNA Sample 1, RNA Sample 2",
            "A visual aid to simplify navigation for the person managing the metadata.");
        case ANALYSIS -> new Helper("Enumeration, Select a value from the dropdown",
            "The test performed on samples for the purpose of finding and measuring chemical substances.");
        case BIOLOGICAL_REPLICATE -> new Helper("Free text, e.g. patient1, patient2, Mouse1", """
            Different samples measured accross multiple conditions.
            Tip: You can use this column to identifiy whether the samples belong to the same source.""");
        case CONDITION -> new Helper("Enumeration, Select a value from the dropdown", """
            A distinct value or condition of the independent variable at which the dependent variable is measured in order to carry out statistical analysis.
            Note: The values in the dropdown are the predefined values from the experimental design.""");
        case SPECIES -> new Helper("Enumeration, Select a value from the dropdown", """
            Scientific name of the organism(s) from which the biological material is derived. E.g. Homo sapiens, Mus musculus.
            Note: The values in the dropdown are the predefined values from the experimental design.""");
        case ANALYTE -> new Helper("Enumeration, Select a value from the dropdown", """
            The chemical substance extracted from the biological material that is identified and measured.
            Note: The values in the dropdown are the predefined values from the experimental design.""");
        case SPECIMEN -> new Helper("Enumeration, Select a value from the dropdown", """
            Name of the biological material from which the analytes would be extracted.
            Note: The values in the dropdown are the predefined values from the experimental design.""");
        case COMMENT -> new Helper("Free text", "Notes about the sample. (Max 500 characters)");
      };
    }
    throw new IllegalArgumentException(
        "Column not of class " + RegisterColumn.class.getName() + " but is "
            + column.getClass().getName());
  };

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  public static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(RegisterColumn::index)
        .max().orElse(0);
  }

  public static Set<String> headerNames() {
    return Arrays.stream(values()).map(RegisterColumn::headerName).collect(Collectors.toSet());
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

  @Override
  public String headerName() {
    return headerName;
  }

  @Override
  public int index() {
    return columnIndex;
  }

  @Override
  public boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public boolean isMandatory() {
    return mandatory;
  }

  @Override
  public Optional<Helper> getFillHelp() {
    return Optional.of(exampleProvider.getHelper(this));
  }
}
