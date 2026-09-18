package life.qbic.datamanager.views.projects.project.measurements.pagination;

/**
 * The measurement domains displayed as paginated lists, mirroring the {@code tab} query
 * parameter of the measurements route ({@code tab=ngs|pxp|ip}).
 *
 * @since 1.19.0
 */
public enum MeasurementDomain {
  NGS("ngs"),
  PXP("pxp"),
  IP("ip");

  private final String urlValue;

  MeasurementDomain(String urlValue) {
    this.urlValue = urlValue;
  }

  /**
   * @return the value used in the URL {@code tab} parameter
   */
  public String urlValue() {
    return urlValue;
  }

  /**
   * Parses a domain from its URL value, ignoring unknown values.
   *
   * @param value the raw {@code tab} parameter, may be {@code null}
   * @return the matching domain or {@link #NGS} when the value is unknown
   */
  public static MeasurementDomain fromUrlValue(String value) {
    if (value == null) {
      return NGS;
    }
    for (MeasurementDomain domain : values()) {
      if (domain.urlValue.equalsIgnoreCase(value)) {
        return domain;
      }
    }
    return NGS;
  }
}