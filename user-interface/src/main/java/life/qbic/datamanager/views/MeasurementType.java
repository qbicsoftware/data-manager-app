package life.qbic.datamanager.views;

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
  GENOMICS("Genomics");

  private final String type;

  MeasurementType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
