package life.qbic.datamanager.views.projects.project.dataset;

import life.qbic.projectmanagement.application.query.AssayType;

/**
 * The measurement methods a <b>Measurement Dataset</b> can hold, as the page segments them.
 *
 * <p>A measurement dataset is the experiment's data for one measurement method, so an experiment
 * has one dataset per method it actually measured — not one dataset in total. This enum is the
 * page's segment axis for exactly that reason.</p>
 *
 * <p>It is deliberately separate from {@link AssayType}, which names the read-model columns and
 * carries no display label. The view owns the words and the tag colour; the application layer owns
 * the data. The switch in {@link #from(AssayType)} stays explicit rather than using
 * {@code valueOf(name())}, so a rename on either side fails at compile time instead of silently
 * mapping to the wrong segment.</p>
 *
 * @since 1.21.0
 */
public enum MeasurementDatasetSegment {

  GENOMICS("Genomics", "genomics", "GENOMICS"),
  PROTEOMICS("Proteomics", "proteomics", "PROTEOMICS"),
  IMMUNOPEPTIDOMICS("Immunopeptidomics", "immunopeptidomics", "IMMUNOPEPTIDOMICS");

  private final String label;
  private final String urlValue;
  private final String apiValue;

  MeasurementDatasetSegment(String label, String urlValue, String apiValue) {
    this.label = label;
    this.urlValue = urlValue;
    this.apiValue = apiValue;
  }

  /** The human-readable segment label, as the tab renders it. */
  public String label() {
    return label;
  }

  /** The stable value this segment carries in the URL. */
  public String urlValue() {
    return urlValue;
  }

  /**
   * The value the read model's query expects for this segment.
   *
   * <p>The read model speaks in {@link AssayType} names; this is that name, so the view never has
   * to build the string itself.</p>
   */
  public String apiValue() {
    return apiValue;
  }

  /**
   * Resolves a segment from its URL value.
   *
   * @param urlValue the value read from the URL
   * @return the matching segment
   * @throws IllegalArgumentException if the value names no segment, so a hand-crafted URL cannot
   *                                  silently render an unrelated segment
   */
  public static MeasurementDatasetSegment fromUrlValue(String urlValue) {
    for (MeasurementDatasetSegment segment : values()) {
      if (segment.urlValue.equals(urlValue)) {
        return segment;
      }
    }
    throw new IllegalArgumentException("Unknown measurement dataset segment: " + urlValue);
  }

  /** Resolves the page's segment for a read-model assay type. */
  public static MeasurementDatasetSegment from(AssayType assayType) {
    return switch (assayType) {
      case GENOMICS -> GENOMICS;
      case PROTEOMICS -> PROTEOMICS;
      case IMMUNOPEPTIDOMICS -> IMMUNOPEPTIDOMICS;
    };
  }
}
