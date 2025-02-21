package life.qbic.datamanager.files.structure.sample;

import java.util.Arrays;
import java.util.Optional;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.ExampleProvider;
import life.qbic.datamanager.files.structure.ExampleProvider.Helper;

public enum InformationColumn implements Column {
  SAMPLE_ID("QBiC Sample Id", 0, true),
  SAMPLE_NAME("Sample Name", 1, true),
  ANALYSIS("Analysis to be performed", 2, true),
  BIOLOGICAL_REPLICATE("Biological Replicate", 3, false),
  CONDITION("Condition", 4, true),
  SPECIES("Species", 5, true),
  SPECIMEN("Specimen", 6, true),
  ANALYTE("Analyte", 7, true),
  COMMENT("Comment", 8, false),
  ;


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
        "Column not of class " + InformationColumn.class.getName() + " but is " + column.getClass()
            .getName());
  };

  private final String name;
  private final int index;
  private final boolean mandatory;

  InformationColumn(String columnName, int columnIndex, boolean mandatory) {
    this.name = columnName;
    this.index = columnIndex;
    this.mandatory = mandatory;
  }

  public static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(Column::index)
        .max().orElse(0);
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public String headerName() {
    return name;
  }

  @Override
  public boolean isMandatory() {
    return mandatory;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public Optional<Helper> getFillHelp() {
    return Optional.ofNullable(exampleProvider.getHelper(this));
  }

}
