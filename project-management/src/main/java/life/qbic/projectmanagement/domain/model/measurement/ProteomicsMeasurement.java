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
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Entity(name = "proteomics_measurement")
public class ProteomicsMeasurement implements MeasurementMetadata {


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

  protected ProteomicsMeasurement() {
    // Needed for JPA
  }

  private ProteomicsMeasurement(Collection<SampleId> sampleIds, MeasurementCode measurementCode,
      ProteomicsMethodMetadata method, ProteomicsSamplePreparation samplePreparation) {
    measuredSamples = new ArrayList<>();
    measuredSamples.addAll(sampleIds);
    this.instrument = method.instrument();
    this.measurementCode = measurementCode;
  }

  private ProteomicsMeasurement(Collection<SampleId> sampleIds, MeasurementCode measurementCode,
      ProteomicsMethodMetadata method) {
    measuredSamples = new ArrayList<>();
    measuredSamples.addAll(sampleIds);
    this.instrument = method.instrument();
    this.measurementCode = measurementCode;
  }

  /**
   * Creates a new {@link ProteomicsMeasurement} object instance, that describes an NGS measurement
   * entity with many describing properties about provenance and instrumentation.
   *
   * @param sampleIds  the sample ids of the samples the measurement was performed on. If more than
   *                   one sample id is provided, the measurement is considered to be performed on a
   *                   pooled sample
   * @param instrument the instrument used for the measurement, which is represented as an
   *                   {@link OntologyTerm}
   * @return
   * @since 1.0.0
   */
  public static ProteomicsMeasurement create(Collection<SampleId> sampleIds,
      MeasurementCode measurementCode, Organisation organisation, ProteomicsMethodMetadata method) {
    if (sampleIds.isEmpty()) {
      throw new IllegalArgumentException(
          "No sample ids provided. At least one sample id must provided for a measurement.");
    }
    Objects.requireNonNull(method.instrument());
    Objects.requireNonNull(measurementCode);
    if (!measurementCode.isNGSDomain()) {
      throw new IllegalArgumentException(
          "Proteomics code is not from the Proteomics domain for: \"" + measurementCode + "\"");
    }
    return new ProteomicsMeasurement(sampleIds, measurementCode, method);
  }

  public ProteomicsMeasurement create(Collection<SampleId> sampleIds, MeasurementCode code,
      Organisation organisation, ProteomicsMethodMetadata method,
      ProteomicsSamplePreparation samplePreparation) {
    var measurement = create(sampleIds, code, organisation, method);
    measurement.setSamplePreparation(samplePreparation);
    return measurement;
  }

  public void setSamplePreparation(ProteomicsSamplePreparation samplePreparation) {

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

}
