package life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for measurement-sample associations.
 *
 * <p>The same sample can participate in multiple measurements, meaning {@code sample_id} alone
 * is not sufficient as a primary key. This embeddable composite key combines {@code sample_id} and
 * {@code measurement_id} to uniquely identify a sample within a specific measurement.
 *
 * <p>Using a composite key ensures Hibernate's first-level cache correctly distinguishes
 * between instances of the same sample appearing in different measurements, preventing incorrect
 * entity resolution from the session cache.
 */
@Embeddable
public class MeasuredSampleId implements Serializable {

  @Column(name = "sample_id")
  private String sampleId;

  @Column(name = "measurement_id")
  private String measurementId;

  protected MeasuredSampleId() {
  }

  public MeasuredSampleId(String sampleId, String measurementId) {
    this.sampleId = sampleId;
    this.measurementId = measurementId;
  }

  /**
   * Returns the sample identifier component of this composite key.
   *
   * @return the sample identifier; never null for a valid persisted entity
   */
  public String sampleId() {
    return sampleId;
  }

  /**
   * Two {@link MeasuredSampleId} instances are equal if and only if both their {@code sampleId} and
   * {@code measurementId} are equal.
   *
   * @param o the object to compare with
   * @return {@code true} if both components match; {@code false} otherwise
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MeasuredSampleId that)) {
      return false;
    }
    return Objects.equals(sampleId, that.sampleId)
        && Objects.equals(measurementId, that.measurementId);
  }

  /**
   * Hash code derived from both {@code sampleId} and {@code measurementId}, consistent with
   * {@link #equals(Object)}.
   *
   * @return the combined hash code
   */
  @Override
  public int hashCode() {
    return Objects.hash(sampleId, measurementId);
  }
}
