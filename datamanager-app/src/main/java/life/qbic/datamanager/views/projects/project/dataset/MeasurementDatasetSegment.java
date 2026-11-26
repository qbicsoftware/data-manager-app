package life.qbic.datamanager.views.projects.project.dataset;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import life.qbic.projectmanagement.application.query.AssayType;

/**
 * <b>One measurement dataset of an experiment, as the page selects and filters it.</b>
 *
 * <p>A measurement dataset is the experiment's data for one <em>omics domain, method and
 * instrument</em>. It used to be an enum of the three omics domains -- which was the wrong grain in
 * two ways. A domain is not a dataset: WES and RNA-Seq are both genomics, so one domain can back
 * several datasets and a domain card cannot count them. And the instrument is part of the identity,
 * because it is the only controlled term that distinguishes two mass-spectrometry datasets of the
 * same nominal method (see #1660).</p>
 *
 * <p>The value is therefore a triple, and the page renders one card per distinct triple, in domain
 * then method then instrument order. That is what keeps a multi-omics experiment legible -- the
 * domains still group the cards, as the grouping they are -- without claiming a domain is a
 * dataset.</p>
 *
 * <p><b>Why a class and not a record key.</b> The identity has a canonical string form, because the
 * selected dataset is addressable in the URL and a link has to survive a reload and a share. One
 * parser and one formatter keep the two directions from drifting: the key is built by
 * {@link #urlValue()} and read back by {@link #fromUrlValue(String)}, and both live here.</p>
 *
 * @since 1.22.0
 */
public final class MeasurementDatasetSegment {

  /**
   * The separator between the three parts of the URL value.
   *
   * <p>A colon, because none of the parts may contain one: the assay type is an enum name, and the
   * method and instrument labels are vocabulary terms. The instrument is the only free-ish value and
   * is a resolved ontology label, so the risk of a colon in it is accepted and handled by splitting
   * on the first two separators only -- a third colon stays part of the instrument.</p>
   */
  private static final String SEPARATOR = ":";

  /** Shown in place of an instrument when the measurement records none. */
  static final String NO_INSTRUMENT = "\u2014";

  private final AssayType assayType;
  private final String methodLabel;
  private final String instrumentLabel;

  private MeasurementDatasetSegment(AssayType assayType, String methodLabel,
      String instrumentLabel) {
    this.assayType = requireNonNull(assayType, "assayType must not be null");
    this.methodLabel = requireNonNull(methodLabel, "methodLabel must not be null");
    this.instrumentLabel = instrumentLabel;
  }

  /**
   * The dataset of one experiment for one domain, method and instrument.
   *
   * @param assayType       the omics domain, from the measurement table the record lives in
   * @param methodLabel     the analysis method, a controlled enum name where one was annotated
   * @param instrumentLabel the instrument recorded on the measurement, or {@code null} when it
   *                        records none
   */
  public static MeasurementDatasetSegment of(AssayType assayType, String methodLabel,
      String instrumentLabel) {
    String method = methodLabel == null || methodLabel.isBlank() ? NO_INSTRUMENT : methodLabel;
    String instrument = instrumentLabel == null || instrumentLabel.isBlank()
        ? null : instrumentLabel;
    return new MeasurementDatasetSegment(assayType, method, instrument);
  }
  /** The omics domain. Groups the cards; is not itself a dataset. */
  public AssayType assayType() {
    return assayType;
  }

  /** The omics domain's display label. */
  public String domainLabel() {
    return switch (assayType) {
      case GENOMICS -> "Genomics";
      case PROTEOMICS -> "Proteomics";
      case IMMUNOPEPTIDOMICS -> "Immunopeptidomics";
    };
  }

  /** The analysis method, as annotated today. */
  public String methodLabel() {
    return methodLabel;
  }

  /** The instrument, or {@code null} when the measurement records none. */
  public String instrumentLabel() {
    return instrumentLabel;
  }

  /** The instrument for display: an em dash where none was recorded, never a substitution. */
  public String instrumentLabelOrDash() {
    return instrumentLabel == null ? NO_INSTRUMENT : instrumentLabel;
  }

  /**
   * The card's identity line: the method, and the instrument beside it.
   *
   * <p>Both are stated because both are needed to tell two datasets apart. A method alone repeats
   * across domains (the same {@code WGS} can sit on a genomics and a proteomics dataset) and an
   * instrument alone repeats across methods on one platform.</p>
   */
  public String identityLabel() {
    return instrumentLabel == null
        ? methodLabel
        : methodLabel + " \u00b7 " + instrumentLabel;
  }

  /**
   * The URL value: {@code DOMAIN:method:instrument}, with an empty instrument part where none was
   * recorded.
   *
   * <p>Comparable by string, so a stale link can be matched against the rendered datasets without a
   * second identity rule.</p>
   */
  public String urlValue() {
    return assayType.name() + SEPARATOR + methodLabel + SEPARATOR
        + (instrumentLabel == null ? "" : instrumentLabel);
  }

  /**
   * Resolves a dataset from its URL value.
   *
   * @param urlValue the value read from the URL
   * @return the dataset
   * @throws IllegalArgumentException if the value is malformed or names no known omics domain, so a
   *                                  hand-crafted link cannot silently render an unrelated dataset
   */
  public static MeasurementDatasetSegment fromUrlValue(String urlValue) {
    requireNonNull(urlValue, "urlValue must not be null");
    String[] parts = urlValue.split(SEPARATOR, 3);
    if (parts.length < 2) {
      throw new IllegalArgumentException("Unknown measurement dataset: " + urlValue);
    }
    AssayType assayType;
    try {
      assayType = AssayType.valueOf(parts[0].toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown measurement dataset: " + urlValue, e);
    }
    String instrument = parts.length == 3 && !parts[2].isBlank() ? parts[2] : null;
    return of(assayType, parts[1], instrument);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MeasurementDatasetSegment that)) {
      return false;
    }
    return assayType == that.assayType
        && methodLabel.equals(that.methodLabel)
        && java.util.Objects.equals(instrumentLabel, that.instrumentLabel);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(assayType, methodLabel, instrumentLabel);
  }

  @Override
  public String toString() {
    return urlValue();
  }
}
