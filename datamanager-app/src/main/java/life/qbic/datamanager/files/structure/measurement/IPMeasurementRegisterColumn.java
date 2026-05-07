package life.qbic.datamanager.files.structure.measurement;

import java.util.Arrays;
import java.util.Optional;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.ExampleProvider;
import life.qbic.datamanager.files.structure.ExampleProvider.Helper;

/**
 * <b>Immunopeptidomics Measurement Register Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for immunopeptidomics measurement
 * registration in the context of measurement file based upload. Provides the name of the header
 * column, the column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum IPMeasurementRegisterColumn implements Column {

  SAMPLE_ID("QBiC Sample Id*", 0, false, true),
  SAMPLE_NAME("Sample Name", 1, true, false),
  MEASUREMENT_NAME("Measurement Name", 2, false, false),
  ORGANISATION_URL("Organisation URL*", 3, false, true),
  FACILITY("Facility*", 4, false, true),
  SAMPLE_MASS("Sample Mass (mg)*", 5, false, true),
  SAMPLE_VOLUME("Sample Volume (decimal)*", 6, false, true),
  CYCLE_FRACTION_NAME("Cycle/Fraction Name", 7, false, false),
  MHC_ANTIBODY("MHC Antibody*", 8, false, true),
  MHC_TYPING_METHOD("MHC Typing Method", 9, false, false),
  ENRICHMENT_METHOD("Enrichment method*", 10, false, true),
  PREP_DATE("Prep Date", 11, false, false),
  MS_RUN_DATE("MS Run Date", 12, false, false),
  INSTRUMENT("Instrument*", 13, false, true),
  LCMS_METHOD("LCMS Method*", 14, false, true),
  LC_COLUMN("LC Column*", 15, false, true),
  DATA_ACQUISITION("Data Acquisition*", 16, false, true),
  MASS_RANGE("Mass range (m/z)*", 17, false, true),
  RETENTION_TIME_RANGE("Retention time range (min)*", 18, false, true),
  CHARGE_RANGE("Charge range*", 19, false, true),
  ION_MOBILITY_RANGE("Ion mobility range (1/k0)", 20, false, false),
  COMMENT("Comment", 21, false, false);

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  private static final ExampleProvider EXAMPLE_PROVIDER = (Column column) -> {
    if (column instanceof IPMeasurementRegisterColumn ipMeasurementRegisterColumn) {
      return switch (ipMeasurementRegisterColumn) {
        case SAMPLE_ID -> new Helper("QBiC sample IDs, e.g. Q2001, Q2002",
            "The sample(s) that will be linked to the measurement.");
        case SAMPLE_NAME -> new Helper("Free text, e.g. Sample 1, Sample 2",
            "A visual aid to simplify sample navigation for the person managing the metadata. Is ignored after upload.");
        case MEASUREMENT_NAME -> new Helper("Free text, e.g. your local identifier for the measurement",
            "Name given for the measurement.");
        case ORGANISATION_URL -> new Helper("ROR URL, e.g. https://ror.org/03a1kwz48",
            """
            A unique identifier of the organisation where the measurement has been conducted.
            Tip: You can click on the column header (%s) to go to the ROR registry website where you can search your organisation and find its ROR URL.
            """.formatted(ORGANISATION_URL.headerName()));
        case FACILITY -> new Helper("Free text, e.g. Quantitative Biology Centre",
            "The facilities name within the organisation (group name, etc.)");
        case SAMPLE_MASS -> new Helper("Decimal number, e.g. 1.5",
            "The mass of the sample in milligrams.");
        case SAMPLE_VOLUME -> new Helper("Decimal number, e.g. 100.5",
            "The volume of the sample in microliters.");
        case CYCLE_FRACTION_NAME -> new Helper("Free text, e.g. Fraction 1",
            "The cycle or fraction name.");
        case MHC_ANTIBODY -> new Helper("Free text, e.g. W6/32",
            "The MHC antibody used for immunopeptidomics.");
        case MHC_TYPING_METHOD -> new Helper("Free text, e.g. PCR-SSP",
            "The MHC typing method used.");
        case ENRICHMENT_METHOD -> new Helper("Free text, e.g. Immune affinity",
            "The enrichment method used to isolate MHC-bound peptides.");
        case PREP_DATE -> new Helper("YYYY-MM-DD, e.g. 2024-01-15",
            "The date the sample was prepared.");
        case MS_RUN_DATE -> new Helper("YYYY-MM-DD, e.g. 2024-01-16",
            "The date the mass spectrometry run was performed.");
        case INSTRUMENT -> new Helper("CURIE (ontology), e.g. EFO:0008637",
            """
            The instrument that has been used for the measurement.
            We expect an ontology term CURIE.
            Tip: You can click on the column header (%s) to go to the Data Manager where you can use our Ontology Search to query the CURIE for your instrument.
            """.formatted(INSTRUMENT.headerName()));
        case LCMS_METHOD -> new Helper("Free text, e.g. DDA",
            "The LC-MS method used for data acquisition.");
        case LC_COLUMN -> new Helper("Free text, e.g. C18",
            "The liquid chromatography column used.");
        case DATA_ACQUISITION -> new Helper("Free text, e.g. DDA, DIA, PRM",
            "The data acquisition method.");
        case MASS_RANGE -> new Helper("Free text, e.g. 300-1800",
            "The mass range in m/z.");
        case RETENTION_TIME_RANGE -> new Helper("Integer, e.g. 120",
            "The retention time range in minutes.");
        case CHARGE_RANGE -> new Helper("Free text, e.g. 2-4",
            "The charge state range.");
        case ION_MOBILITY_RANGE -> new Helper("Free text, e.g. 0.6-1.6",
            "The ion mobility range in 1/k0.");
        case COMMENT -> new Helper("Free text", "Notes about the measurement.");
      };
    } else {
      throw new IllegalArgumentException(
          "Column not of class " + IPMeasurementRegisterColumn.class.getName() + " but is "
              + column.getClass().getName());
    }
  };

  static int maxColumnIndex() {
    return Arrays.stream(values())
        .mapToInt(IPMeasurementRegisterColumn::index)
        .max().orElse(0);
  }

  /**
   * @param headerName  the name in the header
   * @param columnIndex the index of the column this property is in
   * @param readOnly    is the property read only
   * @param mandatory
   */
  IPMeasurementRegisterColumn(String headerName, int columnIndex, boolean readOnly,
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
