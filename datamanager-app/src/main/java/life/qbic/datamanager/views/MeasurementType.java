package life.qbic.datamanager.views;

import life.qbic.projectmanagement.application.query.AssayType;

/**
 * <b>Measurement Type</b>
 * <p>
 * Some controlled enum vocabulary for different measurement types to use in the frontend part of
 * the application.
 *
 * @since 1.6.0
 */
public enum MeasurementType {
  PROTEOMICS("Proteomics"),
  GENOMICS("Genomics"),
  IMMUNOPEPTIDOMICS("Immunopeptidomics");

  private final String type;

  MeasurementType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  /**
   * The view vocabulary for an assay type, so callers never map the read model's types by hand.
   * This enum owns the label and the tag colour; {@link AssayType} owns the read-model columns.
   *
   * <p>The constants deliberately share their names with {@link AssayType}, but the switch stays
   * explicit: {@code valueOf(name())} would break silently the day one side is renamed.</p>
   */
  public static MeasurementType from(AssayType assayType) {
    return switch (assayType) {
      case GENOMICS -> GENOMICS;
      case PROTEOMICS -> PROTEOMICS;
      case IMMUNOPEPTIDOMICS -> IMMUNOPEPTIDOMICS;
    };
  }
}
