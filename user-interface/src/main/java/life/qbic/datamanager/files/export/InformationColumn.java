package life.qbic.datamanager.files.export;

import java.util.Arrays;
import java.util.Optional;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.importing.parser.ExampleProvider;
import life.qbic.datamanager.importing.parser.ExampleProvider.Helper;

/**
 * <b>Sample Edit Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for sample edit
 * in the context of sample batch file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum InformationColumn implements Column {
  SAMPLE_ID("QBiC Sample Id", 0),
  SAMPLE_NAME("Sample Name", 1),
  ANALYSIS("Analysis to be performed", 2),
  BIOLOGICAL_REPLICATE("Biological Replicate", 3),
  CONDITION("Condition", 4),
  SPECIES("Species", 5),
  SPECIMEN("Specimen", 6),
  ANALYTE("Analyte", 7),
  COMMENT("Comment", 8);

  private static final ExampleProvider exampleProvider = column -> {
    if (column instanceof InformationColumn infoColumn) {
      return switch (infoColumn) {
        case SAMPLE_ID -> new Helper("QBiC sample IDs, e.g. Q2001, Q2002",
            "The sample(s) that will be linked to the measurement.");
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
        "Column not of class " + InformationColumn.class.getName() + " but is "
            + column.getClass().getName());
  };
  private final String headerName;
  private final int columnIndex;

  public static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(InformationColumn::getIndex)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   */
  InformationColumn(String headerName, int columnIndex) {
    this.headerName = headerName;
    this.columnIndex = columnIndex;
  }

  @Override
  public String getName() {
    return headerName;
  }

  @Override
  public boolean isMandatory() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public int getIndex() {
    return columnIndex;
  }

  @Override
  public Optional<Helper> getFillHelp() {
    return Optional.ofNullable(exampleProvider.getHelper(this));
  }
}
