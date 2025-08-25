package life.qbic.projectmanagement.infrastructure.template.provider.openxml.column;

import java.util.Optional;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.ExampleProvider;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.ExampleProvider.Helper;

/**
 * <b>Proteomics Measurement Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for proteomics measurement registration
 * in the context of measurement file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet. Also provides
 * information on whether the column is mandatory and can offer some help for filling it.
 * </p>
 */
public enum ProteomicsMeasurementRegisterColumn implements Column {

  SAMPLE_ID("QBiC Sample Id", 0, false, true),
  SAMPLE_NAME(
      "Sample Name", 1, true, false),
  POOL_GROUP("Sample Pool Group", 2, false, false),
  TECHNICAL_REPLICATE_NAME("Technical Replicate", 3, false, false),
  CYCLE_FRACTION_NAME("Cycle/Fraction Name", 4, false, false),
  ORGANISATION_URL("Organisation URL", 5, false, true),
  FACILITY("Facility", 6, false, true),
  LC_COLUMN("LC Column", 7, false, true),
  MS_DEVICE("MS Device", 8, false, true),
  LCMS_METHOD("LCMS Method", 9, false, false),
  DIGESTION_METHOD("Digestion Method", 10, false, true),
  DIGESTION_ENZYME("Digestion Enzyme", 11, false, true),
  ENRICHMENT_METHOD("Enrichment Method", 12, false, false),
  LABELING_TYPE("Labeling Type", 13, false, false),
  LABEL("Label", 14, false, false),
  INJECTION_VOLUME("Injection Volume (µL)", 15, false, false),
  COMMENT("Comment", 16, false, false),
  ;
  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;
  private static final ExampleProvider exampleProvider = (Column column) -> {
    if (column instanceof ProteomicsMeasurementRegisterColumn proteomicsMeasurementRegisterColumn) {
      return switch (proteomicsMeasurementRegisterColumn) {
        case SAMPLE_ID -> new Helper("QBiC sample IDs, e.g. Q29866",
            "The sample(s) that will be linked to the measurement.");
        case SAMPLE_NAME -> new Helper("Free text, e.g. MySample 01",
            "A visual aid to simplify sample navigation for the person managing the metadata. Will be ignored after upload.");
        case POOL_GROUP -> new Helper("Free text, e.g. pool group 1",
            "A group of samples that are pooled together for a measurement. All samples in a pool group should have the same label.");
        case TECHNICAL_REPLICATE_NAME -> new Helper("Free text, e.g. Sample 1A, Sample 1B",
            "Repeated measurements of the same sample that represent independent measures of the random noise associated with protocols or equipment.");
        case ORGANISATION_URL -> new Helper("ROR URL, e.g. https://ror.org/03a1kwz48", """
            A unique identifier of the organisation where the measurement has been conducted.
            Tip: You can click on the column header (%s) to go to the ROR registry website where you can search your organisation and find its ROR URL.
            """.formatted(ORGANISATION_URL.headerName()));
        case FACILITY -> new Helper("Free text, e.g. Quantitative Biology Center",
            "The facility's name within the organisation.");
        case MS_DEVICE -> new Helper("CURIE (ontology), e.g. NCIT:C12434", """
            The instrument that has been used for the measurement.
            We expect an ontology term CURIE.
            Tip: You can click on the column header (%s) to go to the Data Manager where you can use our Ontology Search to query the CURIE for your instrument.
            """.formatted(MS_DEVICE.headerName()));
        case CYCLE_FRACTION_NAME -> new Helper("Free text, e.g. Fraction01, AB",
            "Sometimes a sample is fractionated and all fractions are measured. With this property you can indicate which fraction it is.");
        case DIGESTION_METHOD -> new Helper("Enumeration, Select a value from the dropdown",
            "Method that has been used to break proteins into peptides. Please use the dropdown menu to select one of the values.");
        case DIGESTION_ENZYME -> new Helper("Free text, e.g. Trypsin, Chymotrypsin",
            "Information about the enzymes used for the proteolytic.");
        case ENRICHMENT_METHOD -> new Helper("Free text, e.g. Phosphopeptide Enrichment",
            "Enrichment of proteins or peptides of different characteristics.");
        case INJECTION_VOLUME -> new Helper("Whole number, e.g. 1,6,8",
            "The sample volume injected into the LC column in microliter.");
        case LC_COLUMN -> new Helper("Free text, can be a commercial name or brand",
            "The type of column that has been used.");
        case LCMS_METHOD -> new Helper("Free text",
            "Laboratory specific methods that have been used for LCMS measurement.");
        case LABELING_TYPE -> new Helper("Free text, e.g. Dimethyl, SILAC",
            "The label type that has been used to label the sample for measurement.");
        case LABEL -> new Helper("Free text, e.g. Light, Medium, Heavy",
            "The label value for the label type that has been used.");
        case COMMENT ->
            new Helper("Free text", "Notes about the measurement. (Max 500 characters)");
      };
    } else {
      throw new IllegalArgumentException(
          "Column not of class " + NGSMeasurementRegisterColumn.class.getName() + " but is "
              + column.getClass().getName());
    }
  };

  ProteomicsMeasurementRegisterColumn(String headerName, int columnIndex, boolean readOnly,
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
    return Optional.ofNullable(exampleProvider.getHelper(this));
  }
}
