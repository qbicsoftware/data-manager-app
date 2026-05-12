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

  SAMPLE_ID("QBiC Sample Id", 0, false, true),
  SAMPLE_NAME("Sample Name", 1, true, false),
  MEASUREMENT_NAME("Measurement Name", 2, false, false),
  CYCLE_FRACTION_NAME("Cycle/Fraction Name", 3, false, false),
  SAMPLE_MASS("Sample Mass (mg)", 4, false, true),
  SAMPLE_VOLUME("Sample Volume (decimal)", 5, false, true),
  PREP_DATE("Prep Date", 6, false, false),
  ENRICHMENT_METHOD("Enrichment method", 7, false, true),
  MHC_ANTIBODY("MHC Antibody", 8, false, true),
  MHC_TYPING_METHOD("MHC Typing Method", 9, false, false),
  FACILITY("Facility", 10, false, true),
  ORGANISATION_URL("Organisation URL", 11, false, true),
  MS_RUN_DATE("MS Run Date", 12, false, false),
  DATA_ACQUISITION("Data Acquisition", 13, false, true),
  INSTRUMENT("Instrument", 14, false, true),
  LCMS_METHOD("LCMS Method", 15, false, true),
  LC_COLUMN("LC Column", 16, false, true),
  CHARGE_RANGE("Charge range", 17, false, true),
  ION_MOBILITY_RANGE("Ion mobility range (1/k0)", 18, false, false),
  MASS_RANGE("Mass range (m/z)", 19, false, true),
  RETENTION_TIME_RANGE("Retention time range (min)", 20, false, true),
  COMMENT("Comment", 21, false, false);

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  private static final ExampleProvider EXAMPLE_PROVIDER = (Column column) -> {
    if (column instanceof IPMeasurementRegisterColumn ipMeasurementRegisterColumn) {
      return switch (ipMeasurementRegisterColumn) {
        case SAMPLE_ID -> new Helper("QBiC sample IDs, e.g. Q2001, Q2002",
            "Each measurement need to be linked to at least on analyte sample.");
        case SAMPLE_NAME -> new Helper("Free text, e.g. Sample 1, Sample 2",
            "Coprocessing Identifier. This is just a visual aid simplify sample navigation for the person managing the metadata. You can e.g. download the sample metadata and copy the sample ID + label column in here. This column gets ignored during measurement registration");
        case MEASUREMENT_NAME -> new Helper("Free text",
            "Internal Identifier used by the partner facility to enable them to track their measurement");
        case ORGANISATION_URL -> new Helper("ROR URL, e.g. https://ror.org/03a1kwz48",
            "For good provenance tracking and enabling FAIR, we need a persistent and unique identifier of the organisation the measurement has been conducted at. We expect a full ROR id as URL (e.g. https://ror.org/03a1kwz48)");
        case FACILITY -> new Helper("Free text, e.g. Quantitative Biology Centre",
            "Ideally the facilites name within the organisation (groupname, etc.)");
        case SAMPLE_MASS -> new Helper("Decimal number, e.g. 1.5",
            "Mass that was harvested from the biological probe (mg)");
        case SAMPLE_VOLUME -> new Helper("Decimal number, e.g. 100.5",
            "Volume after enrichment that was injected into the mass spectrometer (microliter)");
        case CYCLE_FRACTION_NAME -> new Helper("Free text, e.g. Fraction01, AB",
            "Sometimes a sample is fractionated and all fractions are measured. With this property you can indicate which fraction it is.");
        case MHC_ANTIBODY -> new Helper("Free text",
            "The MHC Antibody that was used for the measurement");
        case MHC_TYPING_METHOD -> new Helper("Free text",
            "Method used to obtain the donors HLA typing (e.g. DNA seq-based with Optitype, RNA-seq-based with HLA-LA, Immunopeptidomics-based with immunotype)");
        case ENRICHMENT_METHOD -> new Helper("Free text",
            "Method to enrich HLA peptides in sample volume ( e.g. immunoaffinity purification, immunoaffinity purification (iodoacetamide), mild acid elution, detergent lysis )");
        case PREP_DATE -> new Helper("YYYY-MM-DD",
            "Includes the date of the sample preparation (YYYYMMDD)");
        case MS_RUN_DATE -> new Helper("YYYY-MM-DD",
            "Includes the date of the sample measurement on the MS (YYYYMMDD)");
        case INSTRUMENT -> new Helper("CURIE (ontology), e.g. EFO:0008637",
            "The instrument model that has been used for the measurement, which needs to be an ontology CURIE that will be resolved to an existing persistent ID. You can use the ontology search in the data manager to get the CURIE for an instrument model.");
        case LCMS_METHOD -> new Helper("Free text, e.g. CIDOT, HCDOT, MSV035",
            "Laboratory specific methods that have been used for LCMS measurements (e.g., CIDOT, HCDOT, MSV035..).");
        case LC_COLUMN -> new Helper("Free text, e.g. C18",
            "The type of column that has been used.");
        case DATA_ACQUISITION -> new Helper("Free text, e.g. DDA, DIA, PRM",
            "Mass spectrometer acquisition mode (e.g. DDA, DIA, PRM etc.)");
        case MASS_RANGE -> new Helper("Free text, e.g. 300-1800",
            "Mass window where the mass spectrometer method was designed to analyze precursors. Units are either m/z or Dalton");
        case RETENTION_TIME_RANGE -> new Helper("Integer, e.g. 120",
            "Time of chromatogram gradient (min)");
        case CHARGE_RANGE -> new Helper("Free text, e.g. 2-4",
            "Charge window where the mass spectrometer method was designed to analyze precursors. Units are either m/z or Dalton");
        case ION_MOBILITY_RANGE -> new Helper("Free text, e.g. 0.6-1.6",
            "Ion mobility window where the mass spectrometer method was designed to analyze precursors. Units are 1/k0 or CCS.");
        case COMMENT -> new Helper("Free text",
            "Any other comments that can be noted (issue at the machines, during isolation or if the sample is excluded from the rest of the analysis)");
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
