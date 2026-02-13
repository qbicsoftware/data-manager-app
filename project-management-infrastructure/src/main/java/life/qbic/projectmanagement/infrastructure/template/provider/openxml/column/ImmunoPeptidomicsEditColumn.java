package life.qbic.projectmanagement.infrastructure.template.provider.openxml.column;

import java.util.Optional;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.ExampleProvider;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.ExampleProvider.Helper;

/**
 * Defines the columns of the ImmunoPeptidomics measurement edit spreadsheet.
 *
 * <p>
 * This enum represents the metadata structure used when editing already registered
 * ImmunoPeptidomics measurements via an Excel sheet. Each enum constant corresponds to a column in
 * the generated spreadsheet and defines:
 * </p>
 *
 * <ul>
 *   <li>The visible column header shown to the user</li>
 *   <li>The column index (position in the sheet)</li>
 *   <li>Whether the column is read-only</li>
 *   <li>Whether the column is mandatory</li>
 * </ul>
 *
 * <p>
 * In contrast to the registration spreadsheet, this edit sheet contains
 * an additional {@link #MEASUREMENT_ID} column, which uniquely identifies
 * the existing measurement to be updated. This ensures that modifications
 * are correctly applied to the intended measurement record.
 * </p>
 *
 * <p>
 * The enum implements {@link Column} and is used during spreadsheet
 * generation, validation, and contextual help provisioning.
 * </p>
 */
public enum ImmunoPeptidomicsEditColumn implements Column {
  MEASUREMENT_ID("Measurement ID", 0, true, true),
  SAMPLE_ID("QBiC Sample Id", 0, false, true),
  SAMPLE_NAME("Sample Name", 1, true, false),
  MEASUREMENT_NAME("Measurement Name", 2, false, false),
  ORGANISATION_URL("Organisation URL", 3, false, true),
  FACILITY("Facility", 4, false, true),
  MS_DEVICE("MS Device", 5, false, true),
  LCMS_METHOD("LCMS Method", 6, false, true),
  MHC_ANTIBODY("MHC Antibody", 7, false, false),
  PREP_DATE("Prep Date", 8, false, false),
  MS_RUN_DATE("MS Run Date", 9, false, false),
  SAMPLE_SHARE("Sample Share [%]", 10, false, false),
  COMMENT("Comment", 13, false, false),
  ;

  private final String headerName;
  private final int columnIndex;
  private final boolean readOnly;
  private final boolean mandatory;

  private static final ExampleProvider EXAMPLE_PROVIDER = (Column column) -> {
    if (!(column instanceof ImmunoPeptidomicsEditColumn editColumn)) {
      throw new IllegalArgumentException(
          "Column not of class " + ImmunoPeptidomicsEditColumn.class.getName() + " but is "
              + column.getClass().getName());
    }
    return switch (editColumn) {
      case MEASUREMENT_ID -> new Helper("QBiC Measurement ID",
          "A unique identifier of the measurement that will be linked to each sample.");
      case SAMPLE_ID -> new Helper("QBiC sample IDs, e.g. Q2001, Q2002",
          "The sample(s) that will be linked to the measurement.");
      case SAMPLE_NAME -> new Helper("Free text, e.g. RNA Sample 1, RNA Sample 2",
          "A visual aid to simplify sample navigation for the person managing the metadata. Is ignored after upload.");
      case MEASUREMENT_NAME -> new Helper(
          "Free text, e.g. your local identifier for the measurement",
          "Name given for the measurement.");
      case ORGANISATION_URL -> new Helper("ROR URL, e.g. https://ror.org/03a1kwz48", """
          A unique identifier of the organisation where the measurement has been conducted.
          Tip: You can click on the column header (%s) to go to the ROR registry website where you can search your organisation and find its ROR URL.
          """.formatted(ORGANISATION_URL.headerName()));
      case FACILITY -> new Helper("Free text, e.g. Quantitative Biology Centre",
          "The facilities name within the organisation (group name, etc.)");
      case MS_DEVICE -> new Helper("CURIE (ontology), e.g. NCIT:C12434", """
          The instrument that has been used for the measurement.
          We expect an ontology term CURIE.
          Tip: You can click on the column header (%s) to go to the Data Manager where you can use our Ontology Search to query the CURIE for your instrument.
          """.formatted(MS_DEVICE.headerName()));
      case LCMS_METHOD -> new Helper("Free text, e.g. CIDOT, HCDOT",
          "Laboratory specific methods that have been used for LCMS measurement.");
      case MHC_ANTIBODY -> new Helper("Free text",
          "The MHC Antibody that was used for the measurement");
      case PREP_DATE -> new Helper("Date, e.g. 2025-01-05",
          "The day the sample was prepared.");
      case MS_RUN_DATE -> new Helper("Date, e.g. 2025-01-05",
          "The day the sample was measured on the MS device.");
      case SAMPLE_SHARE -> new Helper("Number, e.g. 50,12",
          "Percentage of the sample mass taken for the measurement. Values from 0 to 100. A total sample mass of 1mg is assumed.");
      case COMMENT -> new Helper("Free text", "Notes about the measurement. (Max 500 characters)");
    };
  };

  ImmunoPeptidomicsEditColumn(String headerName, int columnIndex, boolean readOnly,
      boolean mandatory) {
    this.headerName = headerName;
    this.columnIndex = columnIndex;
    this.readOnly = readOnly;
    this.mandatory = mandatory;
  }

  @Override
  public int index() {
    return columnIndex;
  }

  @Override
  public String headerName() {
    return headerName;
  }

  @Override
  public boolean isMandatory() {
    return mandatory;
  }

  @Override
  public boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public Optional<Helper> getFillHelp() {
    return Optional.ofNullable(EXAMPLE_PROVIDER.getHelper(this));
  }
}
