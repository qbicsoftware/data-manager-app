package life.qbic.datamanager.files.structure.measurement;

import java.util.Arrays;
import java.util.Optional;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.ExampleProvider;
import life.qbic.datamanager.files.structure.ExampleProvider.Helper;

/**
 * <b>IP Measurement Edit Columns</b>
 *
 * <p>Enumeration of the columns shown in the file used for immunopeptidomics measurement edit
 * in the context of measurement file based upload. Provides the name of the header column, the
 * column index and if the column should be set to readOnly in the generated sheet
 * </p>
 */
public enum IPMeasurementEditColumn implements Column {

  MEASUREMENT_ID("Measurement ID", 0, true, true),
  SAMPLE_ID("QBiC Sample Id", 1, true, true),
  SAMPLE_NAME("Sample Name", 2, true, false),
  MEASUREMENT_NAME("Measurement Name", 3, false, false),
  CYCLE_FRACTION_NAME("Cycle/Fraction Name", 4, false, false),
  SAMPLE_MASS("Sample Mass (mg)", 5, false, true),
  SAMPLE_VOLUME("Sample Volume (decimal)", 6, false, true),
  PREP_DATE("Prep Date", 7, false, false),
  ENRICHMENT_METHOD("Enrichment method", 8, false, true),
  MHC_ANTIBODY("MHC Antibody", 9, false, true),
  MHC_TYPING_METHOD("MHC Typing Method", 10, false, false),
  FACILITY("Facility", 11, false, true),
  ORGANISATION_URL("Organisation URL", 12, false, true),
  ORGANISATION_NAME("Organisation Name", 13, true, false),
  MS_RUN_DATE("MS Run Date", 14, false, false),
  DATA_ACQUISITION("Data Acquisition", 15, false, true),
  INSTRUMENT("Instrument", 16, false, true),
  INSTRUMENT_NAME("Instrument Name", 17, true, false),
  LCMS_METHOD("LCMS Method", 18, false, true),
  LC_COLUMN("LC Column", 19, false, true),
  CHARGE_RANGE("Charge range", 20, false, true),
  ION_MOBILITY_RANGE("Ion mobility range (1/k0)", 21, false, false),
  MASS_RANGE("Mass range (m/z)", 22, false, true),
  RETENTION_TIME_RANGE("Retention time range (min)", 23, false, true),
  COMMENT("Comment", 24, false, false);

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;
  private static final ExampleProvider exampleProvider = (Column column) -> {
    if (column instanceof IPMeasurementEditColumn ipMeasurementEditColumn) {
      return switch (ipMeasurementEditColumn) {
        case MEASUREMENT_ID -> new Helper("QBiC Measurement ID",
            "A unique identifier of the measurement that will be linked to each sample.");
        case SAMPLE_ID -> new Helper("QBiC sample IDs, e.g. Q2001, Q2002",
            "The sample(s) that will be linked to the measurement.");
        case SAMPLE_NAME -> new Helper("Free text, e.g. MySample 01",
            "A visual aid to simplify sample navigation for the person managing the metadata. Will be ignored after upload.");
        case MEASUREMENT_NAME -> new Helper("Free text, e.g. your local identifier for the measurement", "Name given for the measurement.");
        case CYCLE_FRACTION_NAME -> new Helper("Free text, e.g. Fraction01, AB",
            "Sometimes a sample is fractionated and all fractions are measured. With this property you can indicate which fraction it is.");
        case SAMPLE_MASS -> new Helper("Decimal number, e.g. 1.5",
            "Mass that was harvested from the biological probe (mg)");
        case SAMPLE_VOLUME -> new Helper("Decimal number, e.g. 100.5",
            "Volume after enrichment that was injected into the mass spectrometer (microliter)");
        case PREP_DATE -> new Helper("YYYY-MM-DD",
            "Includes the date of the sample preparation (YYYYMMDD)");
        case ENRICHMENT_METHOD -> new Helper("Free text",
            "Method to enrich HLA peptides in sample volume ( e.g. immunoaffinity purification, immunoaffinity purification (iodoacetamide), mild acid elution, detergent lysis )");
        case MHC_ANTIBODY -> new Helper("Free text",
            "The MHC Antibody that was used for the measurement");
        case MHC_TYPING_METHOD -> new Helper("Free text",
            "Method used to obtain the donors HLA typing (e.g. DNA seq-based with Optitype, RNA-seq-based with HLA-LA, Immunopeptidomics-based with immunotype)");
        case FACILITY -> new Helper("Free text, e.g. Quantitative Biology Center",
            "Ideally the facilities name within the organisation (groupname, etc.)");
        case ORGANISATION_URL -> new Helper("ROR URL, e.g. https://ror.org/03a1kwz48",
            "A unique identifier of the organisation where the measurement has been conducted. We expect a full ROR id as URL (e.g. https://ror.org/03a1kwz48)");
        case ORGANISATION_NAME -> new Helper("Free text, e.g. University of Tübingen",
            "The name of the organisation where the measurement has been conducted.");
        case MS_RUN_DATE -> new Helper("YYYY-MM-DD",
            "Includes the date of the sample measurement on the MS (YYYYMMDD)");
        case DATA_ACQUISITION -> new Helper("Free text, e.g. DDA, DIA, PRM",
            "Mass spectrometer acquisition mode (e.g. DDA, DIA, PRM etc.)");
        case INSTRUMENT -> new Helper("CURIE (ontology), e.g. EFO:0008637",
            "The instrument model that has been used for the measurement, which needs to be an ontology CURIE that will be resolved to an existing persistent ID.");
        case INSTRUMENT_NAME -> new Helper("Free text, e.g. Illumina HiSeq",
            "The name of the instrument model that has been used for the measurement.");
        case LCMS_METHOD -> new Helper("Free text, e.g. CIDOT, HCDOT, MSV035",
            "Laboratory specific methods that have been used for LCMS measurements (e.g., CIDOT, HCDOT, MSV035..).");
        case LC_COLUMN -> new Helper("Free text, e.g. C18",
            "The type of column that has been used.");
        case CHARGE_RANGE -> new Helper("Free text, e.g. 2-4",
            "Charge window where the mass spectrometer method was designed to analyze precursors. Units are either m/z or Dalton");
        case ION_MOBILITY_RANGE -> new Helper("Free text, e.g. 0.6-1.6",
            "Ion mobility window where the mass spectrometer method was designed to analyze precursors. Units are 1/k0 or CCS.");
        case MASS_RANGE -> new Helper("Free text, e.g. 300-1800",
            "Mass window where the mass spectrometer method was designed to analyze precursors. Units are either m/z or Dalton");
        case RETENTION_TIME_RANGE -> new Helper("Integer, e.g. 120",
            "Time of chromatogram gradient (min)");
        case COMMENT -> new Helper("Free text",
            "Any other comments that can be noted (issue at the machines, during isolation or if the sample is excluded from the rest of the analysis)");
      };
    } else {
      throw new IllegalArgumentException(
          "Column not of class " + IPMeasurementEditColumn.class.getName() + " but is "
              + column.getClass().getName());
    }
  };

  IPMeasurementEditColumn(String headerName, int columnIndex, boolean readOnly,
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

  static int maxColumnIndex() {
    return Arrays.stream(values()).mapToInt(IPMeasurementEditColumn::index).max().orElse(0);
  }
}