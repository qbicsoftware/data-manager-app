package life.qbic.projectmanagement.application.query;

import java.util.Objects;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;

/**
 * <b>One primary dataset of a project, as the overview states it.</b>
 *
 * <p>A primary dataset is the data of <em>one experiment</em> for <em>one analysis method</em>. It is
 * not declared by a human: it is the consequence of measurements having been done, and its type is
 * the analysis method, because that is what a reader picks datasets by when they plan an analysis
 * (variant calling, cluster analysis, ...). An experiment measured as WES and as RNA-Seq contributes
 * two datasets; two experiments both measured as WES contribute two as well -- which is why the pair
 * (experiment, analysis method) is the identity, and a bare count of analysis methods would not do.
 *
 * <p><b>Where the analysis method comes from.</b> Today it is annotated on the sample, which is a
 * known grandfather relation: the annotation belongs on the measurement and will move there. This
 * record deliberately does not say where the value came from -- the read model resolves it once, so
 * that when the measurement carries its own annotation only that resolution changes and every
 * consumer of this record keeps working. See ADR 0010.</p>
 *
 * <p><b>Attribution of a measurement to more than one method.</b> A measurement can measure samples
 * carrying different analysis methods. Such a measurement is attributed to each of them and is
 * therefore counted in each, which means the sum of {@link #measurements()} across the datasets of a
 * project can exceed the number of measurement records. This follows the precedent already documented
 * for measurements spanning experiments (see the read-model migration, trap 3) and is stated here
 * because it is the one place a reader could otherwise conclude the numbers are wrong.</p>
 *
 * @param experimentId     the experiment the dataset belongs to
 * @param experimentName   the experiment's name, the discriminant between two datasets of one method
 * @param analysisMethod   the analysis method, which is the dataset's type
 * @param measurements     the number of measurements attributed to this dataset
 * @param withPrimaryData  the number of those measurements that carry primary data
 * @param primaryDataBytes the size of the primary data, {@code 0} when nothing arrived or the sync
 *                         never recorded a size (the two are indistinguishable -- see the migration's
 *                         trap 4d, so callers must not render it as "0 B")
 * @since 1.22.0
 */
public record AssayStanding(
    String experimentId,
    String experimentName,
    AnalysisMethod analysisMethod,
    int measurements,
    int withPrimaryData,
    long primaryDataBytes) {

  public AssayStanding {
    Objects.requireNonNull(experimentId, "experimentId must not be null");
    Objects.requireNonNull(experimentName, "experimentName must not be null");
    Objects.requireNonNull(analysisMethod, "analysisMethod must not be null");
    if (measurements < 0 || withPrimaryData < 0) {
      throw new IllegalArgumentException("counts must not be negative");
    }
  }

  /** The number of measurements of this dataset that still lack primary data. */
  public int missingPrimaryData() {
    return Math.max(0, measurements - withPrimaryData);
  }

  /**
   * Whether every measurement of this dataset carries primary data.
   *
   * <p>A dataset without measurements is not complete: it is not a dataset yet. Completeness is a
   * property of data that exists, and reporting "complete" for nothing would be the vacuous truth
   * that makes an empty project look finished.</p>
   */
  public boolean complete() {
    return measurements > 0 && withPrimaryData >= measurements;
  }

  /** The omics domain this analysis method belongs to, for grouping in the overview. */
  public AssayType assayType() {
    return AssayType.of(analysisMethod);
  }
}
