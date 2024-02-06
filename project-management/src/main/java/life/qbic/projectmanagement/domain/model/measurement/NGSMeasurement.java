package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;

/**
 * <b>NGS Measurement Metadata Object</b>
 *
 * <p>Captures an measurement metadata object entity with information
 * about the origin of measurement, the instrumentation and much more domain-specific
 * information.</p>
 * <p>
 * A measurement object can be linked to one or more samples via the samples unique sample id. In
 * the case of originating from more than one sample, we consider this as a typical use case for
 * <strong>sample pooling</strong>.
 *
 * @since 1.0.0
 */
@Entity(name = "ngs_measurements")
public class NGSMeasurement {

  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "measurement_id"))
  private MeasurementId id;

  @ElementCollection
  @CollectionTable(name="measurement_samples", joinColumns = @JoinColumn(name = "measurement_id"))
  @Column(name="measured_sample")
  private Collection<String> measuredSamples;

  protected NGSMeasurement() {
    // Needed for JPA
  }

  private NGSMeasurement(Collection<String> sampleIds) {
    measuredSamples = new ArrayList<>();
    measuredSamples.addAll(sampleIds);
  }

  public static NGSMeasurement create(Collection<String> sampleIds) {
    if (sampleIds.isEmpty()) {
      throw new IllegalArgumentException(
          "No sample ids provided. At least one sample id must provided for a measurement.");
    }
    return new NGSMeasurement(sampleIds);
  }

}
