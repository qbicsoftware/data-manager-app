package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>IP Measurement Entry</b>
 * <p>
 * Maps an immunopeptidomics measurement to a sample. For IP there are no sample-specific
 * properties beyond the sample identifier itself; all measurement metadata lives at the
 * measurement level. This embeddable is kept to preserve the same structural pattern used
 * for NGS and proteomics, making future per-sample fields easy to add if needed.
 */
@Embeddable
public class IPMeasurementEntry {

  @Column(name = "sample_id")
  private SampleId measuredSample;

  protected IPMeasurementEntry() {
  }

  private IPMeasurementEntry(SampleId measuredSample) {
    this.measuredSample = measuredSample;
  }

  public static IPMeasurementEntry create(SampleId sampleId) {
    Objects.requireNonNull(sampleId, "Sample Id cannot be null");
    return new IPMeasurementEntry(sampleId);
  }

  public SampleId measuredSample() {
    return measuredSample;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IPMeasurementEntry that = (IPMeasurementEntry) o;
    return Objects.equals(measuredSample, that.measuredSample);
  }

  @Override
  public int hashCode() {
    return Objects.hash(measuredSample);
  }
}
