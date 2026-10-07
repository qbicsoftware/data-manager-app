package life.qbic.datamanager.views.projects.project.rawdata.pagination;

/**
 * The raw dataset domains displayed as paginated lists, mirroring the {@code tab} query
 * parameter of the raw data route ({@code tab=ngs|pxp|ip}).
 *
 * @since 1.19.0
 */
public enum RawDataDomain {
  NGS("ngs"),
  PXP("pxp"),
  IP("ip");

  private final String urlValue;

  RawDataDomain(String urlValue) {
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
  public static RawDataDomain fromUrlValue(String value) {
    if (value == null) {
      return NGS;
    }
    for (RawDataDomain domain : values()) {
      if (domain.urlValue.equalsIgnoreCase(value)) {
        return domain;
      }
    }
    return NGS;
  }
}