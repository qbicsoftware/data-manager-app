package life.qbic.projectmanagement.infrastructure.template.provider.openxml.column;

import java.util.Arrays;
import java.util.Optional;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.ExampleProvider;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.ExampleProvider.Helper;

/**
 * <b>NGS Measurement Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for NGS measurement registration
 * in the context of measurement file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum NGSMeasurementRegisterColumn implements Column {

  SAMPLE_ID("QBiC Sample Id", 0, false, true),
  SAMPLE_NAME("Sample Name", 1, true, false),
  MEASUREMENT_NAME("Measurement Name", 2, true, false),
  POOL_GROUP("Sample Pool Group", 3, false, false),
  ORGANISATION_URL("Organisation URL", 4, false, true),
  FACILITY("Facility", 5, false, true),
  INSTRUMENT("Instrument", 6, false, true),
  SEQUENCING_READ_TYPE("Sequencing Read Type", 7, false, true),
  LIBRARY_KIT("Library Kit", 8, false, false),
  FLOW_CELL("Flow Cell", 9, false, false),
  SEQUENCING_RUN_PROTOCOL("Sequencing Run Protocol", 10, false, false),
  INDEX_I7("Index i7", 11, false, false),
  INDEX_I5("Index i5", 12, false, false),
  COMMENT("Comment", 13, false, false),
  ;

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  private static final ExampleProvider EXAMPLE_PROVIDER = (Column column) -> {
    if (column instanceof NGSMeasurementRegisterColumn ngsMeasurementRegisterColumn) {
      return switch (ngsMeasurementRegisterColumn) {
        case SAMPLE_ID -> new Helper("QBiC sample IDs, e.g. Q2001, Q2002",
            "The sample(s) that will be linked to the measurement.");
        case SAMPLE_NAME -> new Helper("Free text, e.g. RNA Sample 1, RNA Sample 2",
            "A visual aid to simplify sample navigation for the person managing the metadata. Is ignored after upload.");
        case MEASUREMENT_NAME -> new Helper("Free text, e.g. your local identifier for the measurement", "Name given for the measurement.");
        case POOL_GROUP -> new Helper("Free text, e.g. pool group 1",
            "A group of samples that are pooled together for a measurement. All samples in a pool group should have the same label.");
        case ORGANISATION_URL -> new Helper("ROR URL, e.g. https://ror.org/03a1kwz48", """
            A unique identifier of the organisation where the measurement has been conducted.
            Tip: You can click on the column header (%s) to go to the ROR registry website where you can search your organisation and find its ROR URL.
            """.formatted(ORGANISATION_URL.headerName()));
        case FACILITY -> new Helper("Free text, e.g. Quantitative Biology Centre",
            "The facilities name within the organisation (group name, etc.)");
        case INSTRUMENT -> new Helper("CURIE (ontology), e.g. EFO:0008637", """
            The instrument that has been used for the measurement.
            We expect an ontology term CURIE.
            Tip: You can click on the column header (%s) to go to the Data Manager where you can use our Ontology Search to query the CURIE for your instrument.
            """.formatted(INSTRUMENT.headerName()));
        case SEQUENCING_READ_TYPE -> new Helper("Free text, e.g. paired-end",
            "The sequencing read type used to generate the sequence data.");
        case LIBRARY_KIT -> new Helper("Free text, e.g. NEBNext Ultra II Directional RNA mRNA UMI",
            "Provides important information for downstream analysis data use that is usually required for troubleshooting.");
        case FLOW_CELL ->
            new Helper("Free text, e.g. S4", "The flow cell type used for sequencing.");
        case SEQUENCING_RUN_PROTOCOL -> new Helper("Free text, e.g. 104+19+10+104",
            "Information on how many cycles for each read and index.");
        case INDEX_I7 -> new Helper("Free text, e.g. NEBNext UDI UMI Set 1 B12 S789",
            "Index used for multiplexing.");
        case INDEX_I5 -> new Helper("Free text, e.g. NEBNext UDI UMI Set 1 B12 S579",
            "Index used for multiplexing.");
        case COMMENT ->
            new Helper("Free text", "Notes about the measurement. (Max 500 characters)");
      };
    } else {
      throw new IllegalArgumentException(
          "Column not of class " + NGSMeasurementRegisterColumn.class.getName() + " but is "
              + column.getClass().getName());
    }
  };

  static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(NGSMeasurementRegisterColumn::index)
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
    return Optional.ofNullable(EXAMPLE_PROVIDER.getHelper(this));
  }
}
