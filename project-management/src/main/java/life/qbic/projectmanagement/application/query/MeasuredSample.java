package life.qbic.projectmanagement.application.query;

import java.util.List;

/**
 * One sample a measurement measures.
 *
 * <p>Resolved on demand for a single measurement rather than as part of the list read model: a
 * proteomics measurement can pool fifty or more samples, and a list query that carried every pool's
 * member list would either be enormous or silently truncated. MariaDB's {@code
 * group_concat_max_len} defaults to 1024 bytes, which is roughly 48 sample codes, so a
 * {@code GROUP_CONCAT} approach loses samples past that point <em>without raising an error</em>.
 * Fetching per record when the detail panel opens keeps the list cheap and the panel complete.</p>
 *
 * <p>{@code sampleCode} is the human-facing identifier; {@code sampleLabel} is the name the user
 * registered, which is what a reader recognises in a pool listing.</p>
 *
 * @since 1.21.0
 */
public record MeasuredSample(String sampleId, String sampleCode, String sampleLabel) {

  public MeasuredSample {
    if (sampleId == null) {
      throw new NullPointerException("sampleId must not be null");
    }
  }

  /** A defensive copy helper, since callers hand these lists to the UI. */
  public static List<MeasuredSample> copyOf(List<MeasuredSample> samples) {
    return samples == null ? List.of() : List.copyOf(samples);
  }
}
