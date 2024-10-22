package life.qbic.datamanager.views;

/**
 * <b><enum short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
