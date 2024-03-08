package life.qbic.projectmanagement.domain.model.measurement;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>NGS NGSMeasurementMetadata Metadata Object</b>
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
public class NGSMeasurement implements MeasurementMetadata {

  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "measurement_id"))
  private MeasurementId id;

  @Embedded
  private OntologyTerm instrument;

  @Embedded
  private MeasurementCode measurementCode;

  @ElementCollection
  @CollectionTable(name = "measurement_samples", joinColumns = @JoinColumn(name = "measurement_id"))
  @Column(name = "measured_sample")
  private Collection<SampleId> measuredSamples;

  protected NGSMeasurement() {
    // Needed for JPA
  }

  private NGSMeasurement(Collection<SampleId> sampleIds, MeasurementCode measurementCode, OntologyTerm instrument) {
    measuredSamples = new ArrayList<>();
    measuredSamples.addAll(sampleIds);
    this.instrument = instrument;
    this.measurementCode = measurementCode;
  }

  /**
   * Creates a new {@link NGSMeasurement} object instance, that describes an NGS measurement entity
   * with many describing properties about provenance and instrumentation.
   *
   * @param sampleIds  the sample ids of the samples the measurement was performed on. If more than
   *                   one sample id is provided, the measurement is considered to be performed on a
   *                   pooled sample
   * @param instrument the instrument used for the measurement, which is represented as an
   *                   {@link OntologyTerm}
   * @return
   * @since 1.0.0
   */
  public static NGSMeasurement create(Collection<SampleId> sampleIds, MeasurementCode measurementCode, OntologyTerm instrument) {
    if (sampleIds.isEmpty()) {
      throw new IllegalArgumentException(
          "No sample ids provided. At least one sample id must provided for a measurement.");
    }
    Objects.requireNonNull(instrument);
    Objects.requireNonNull(measurementCode);
    if (!measurementCode.isNGSDomain()) {
      throw new IllegalArgumentException("NGSMeasurementMetadata code is not from the NGS domain for: \"" + measurementCode + "\"");
    }
    return new NGSMeasurement(sampleIds, measurementCode, instrument);
  }

  /**
   * Convenience method to query if the measurement was derived from a pooled sample.
   *
   * @return true, if the measurement was performed on a pooled sample, else returns false
   * @since 1.0.0
   */
  public boolean isPooledSampleMeasurement() {
    return measuredSamples.size() > 1;
  }

  public MeasurementCode measurementCode() {
    return this.measurementCode;
  }

  public MeasurementId measurementId() {
    return id;
  }

  public Collection<SampleId> measuredSamples() {
    return measuredSamples;
  }

  public OntologyTerm instrument() {
    return instrument;
  }
}
