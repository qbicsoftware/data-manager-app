package life.qbic.projectmanagement.domain.model.measurement;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementCreatedEvent;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementUpdatedEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Immunopeptidomics Measurement</b>
 *
 * <p>Captures an immunopeptidomics measurement metadata entity with information about the origin
 * of measurement, the instrumentation and much more domain-specific information.</p>
 * <p>
 * A measurement object can be linked to one or more samples via the samples unique sample id. In
 * the case of originating from more than one sample, we consider this as a typical use case for
 * <strong>sample pooling</strong>.
 *
 * @since 1.11.0
 */
@Entity(name = "ip_measurements")
public class ImmunopeptidomicsMeasurement {

  @Column(name = "facility")
  String facility = "";
  @Column(name = "mhcAntibody")
  String mhcAntibody = "";
  @Column(name = "mhcTypingMethod")
  String mhcTypingMethod = "";
  @Column(name = "enrichmentMethod")
  String enrichmentMethod = "";
  @Column(name = "lcmsMethod")
  String lcmsMethod = "";
  @Column(name = "lcColumn")
  String lcColumn = "";
  @Column(name = "dataAcquisition")
  String dataAcquisition = "";
  @Column(name = "massRange")
  String massRange = "";
  @Column(name = "retentionTimeRange")
  Integer retentionTimeRange = 0;
  @Column(name = "chargeRange")
  String chargeRange = "";
  @Column(name = "ionMobilityRange")
  String ionMobilityRange = "";
  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "measurement_id"))
  private MeasurementId measurementId;
  @Embedded
  @Column(nullable = false)
  private ProjectId projectId;
  @Column(name = "instrument", columnDefinition = "longtext CHECK (json_valid(`instrument`))")
  private OntologyTerm instrument;
  @Column(name = "instrumentName")
  private String instrumentName = "";
  @Column(name = "samplePool")
  private String samplePool = "";
  @Convert(converter = MeasurementCode.MeasurementCodeConverter.class)
  private MeasurementCode measurementCode;
  @Column(name = "registrationTime")
  private Instant registration;
  @Embedded
  private Organisation organisation;

  @Column(name = "measurementName", nullable = false)
  private String measurementName;

  @ElementCollection(targetClass = IPMeasurementEntry.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "specific_measurement_metadata_ip", joinColumns = @JoinColumn(name = "measurement_id"))
  private Set<IPMeasurementEntry> specificMetadata = new HashSet<>();

  protected ImmunopeptidomicsMeasurement() {
    // Needed for JPA
  }

  private ImmunopeptidomicsMeasurement(MeasurementId measurementId, ProjectId projectId,
      String samplePool, MeasurementCode measurementCode, String measurementName,
      Organisation organisation, IPMethodMetadata method, Instant registration,
      Collection<IPMeasurementEntry> measurementMetadata) {
    if (!measurementCode.isIPDomain()) {
      throw new IllegalArgumentException(
          "ImmunopeptidomicsMeasurement code is not from the IP domain for: \"" + measurementCode
              + "\"");
    }
    evaluateMandatoryMetadata(
        method); // throws IllegalArgumentException if required properties are missing
    this.measurementId = measurementId;
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    this.organisation = requireNonNull(organisation, "organisation must not be null");
    this.instrument = requireNonNull(method.instrument(), "instrument must not be null");
    this.instrumentName = method.instrumentName() != null ? method.instrumentName() : "";
    this.measurementCode = requireNonNull(measurementCode, "measurement code must not be null");
    this.facility = requireNonNull(method.facility(), "facility must not be null");
    this.registration = registration;
    this.samplePool = samplePool;
    this.specificMetadata = new HashSet<>(measurementMetadata);
    this.measurementName = Optional.ofNullable(measurementName).orElse("");
    emitCreatedEvent();
  }

  private static void evaluateMandatoryMetadata(IPMethodMetadata method)
      throws IllegalArgumentException {
    if (method.facility().isBlank()) {
      throw new IllegalArgumentException("Facility: Missing metadata");
    }
    if (method.instrument() == null) {
      throw new IllegalArgumentException("Instrument: Missing metadata");
    }
  }

  /**
   * Creates a new pooled {@link ImmunopeptidomicsMeasurement} object instance, that describes an IP
   * measurement entity with many describing properties about provenance and instrumentation.
   *
   * @param projectId                   the project id the measurement belongs to
   * @param samplePool                  the sample pool label the measurement represents
   * @param measurementCode             the assigned measurement code for the measurement
   * @param organisation                where the measurement has been conducted
   * @param method                      measurement method related metadata
   * @param specificMeasurementMetadata sample specific metadata
   * @return an instance of an {@link ImmunopeptidomicsMeasurement}
   * @since 1.11.0
   */
  public static ImmunopeptidomicsMeasurement createWithPool(ProjectId projectId, String samplePool,
      MeasurementCode measurementCode, String measurementName, Organisation organisation,
      IPMethodMetadata method, Collection<IPMeasurementEntry> specificMeasurementMetadata)
      throws IllegalArgumentException {
    requireNonNull(measurementCode, "measurement Code must not be null");
    requireNonNull(method, "method must not be null");
    requireNonNull(method.instrument(), "instrument must not be null");
    if (samplePool.isBlank()) {
      throw new IllegalArgumentException("Sample Pool: no value provided");
    }
    var measurementId = MeasurementId.create();
    return new ImmunopeptidomicsMeasurement(measurementId, projectId, samplePool, measurementCode,
        measurementName, organisation, method, Instant.now(), specificMeasurementMetadata);
  }

  /**
   * Creates a new single {@link ImmunopeptidomicsMeasurement} object instance, that describes an IP
   * measurement entity with many describing properties about provenance and instrumentation.
   *
   * @param projectId                   the project id the measurement belongs to
   * @param measurementCode             the assigned measurement code for the measurement
   * @param organisation                where the measurement has been conducted
   * @param method                      measurement method related metadata
   * @param specificMeasurementMetadata sample specific metadata
   * @return an instance of an {@link ImmunopeptidomicsMeasurement}
   * @since 1.11.0
   */
  public static ImmunopeptidomicsMeasurement createSingleMeasurement(ProjectId projectId,
      MeasurementCode measurementCode, String measurementName, Organisation organisation,
      IPMethodMetadata method, IPMeasurementEntry specificMeasurementMetadata) {
    requireNonNull(measurementCode, "Measurement Code cannot be null");
    requireNonNull(method, "IPMethodMetadata cannot be null");
    requireNonNull(method.instrument(), "Instrument cannot be null");
    return new ImmunopeptidomicsMeasurement(MeasurementId.create(), projectId, "", measurementCode,
        measurementName, organisation, method, Instant.now(), List.of(specificMeasurementMetadata));
  }

  public void setMethod(IPMethodMetadata methodMetadata) {
    this.instrument = methodMetadata.instrument();
    this.instrumentName = methodMetadata.instrumentName() != null ? methodMetadata.instrumentName() : "";
    this.facility = methodMetadata.facility();
    emitUpdatedEvent();
  }

  public void setSpecificMetadata(Collection<IPMeasurementEntry> specificMetadata) {
    this.specificMetadata = new HashSet<>(specificMetadata);
    emitUpdatedEvent();
  }

  public Collection<IPMeasurementEntry> specificMeasurementMetadata() {
    return specificMetadata.stream().toList();
  }

  public void setOrganisation(Organisation organisation) {
    this.organisation = Objects.requireNonNull(organisation);
    emitUpdatedEvent();
  }

  public void updateMethod(IPMethodMetadata methodMetadata) {
    Objects.requireNonNull(methodMetadata);
    setMethod(methodMetadata);
  }

  /**
   * Convenience method to query if the measurement was derived from a single sample.
   *
   * @return true, if the measurement was performed on a single sample, else returns false if the
   * measurement was derived from pooled samples
   * @since 1.11.0
   */
  public boolean isSingleSampleMeasurement() {
    return specificMetadata.size() <= 1;
  }


  public MeasurementCode measurementCode() {
    return this.measurementCode;
  }

  public MeasurementId measurementId() {
    return measurementId;
  }

  public ProjectId projectId() {
    return projectId;
  }

  public OntologyTerm instrument() {
    return instrument;
  }

  public Optional<String> instrumentName() {
    return instrumentName.isBlank() ? Optional.empty() : Optional.of(instrumentName);
  }

  public void setSamplePoolGroup(String group) {
    this.samplePool = group;
  }

  public Optional<String> samplePoolGroup() {
    return samplePool.isBlank() ? Optional.empty() : Optional.of(samplePool);
  }

  public Instant registrationDate() {
    return registration;
  }

  public Organisation organisation() {
    return organisation;
  }

  public String facility() {
    return facility;
  }

  public String mhcAntibody() {
    return mhcAntibody;
  }

  public Optional<String> mhcTypingMethod() {
    return mhcTypingMethod.isBlank() ? Optional.empty() : Optional.of(mhcTypingMethod);
  }

  public String enrichmentMethod() {
    return enrichmentMethod;
  }

  public String lcmsMethod() {
    return lcmsMethod;
  }

  public String lcColumn() {
    return lcColumn;
  }

  public String dataAcquisition() {
    return dataAcquisition;
  }

  public String massRange() {
    return massRange;
  }

  public Integer retentionTimeRange() {
    return retentionTimeRange;
  }

  public String chargeRange() {
    return chargeRange;
  }

  public Optional<String> ionMobilityRange() {
    return ionMobilityRange.isBlank() ? Optional.empty() : Optional.of(ionMobilityRange);
  }

  public Collection<SampleId> measuredSamples() {
    return specificMetadata.stream().map(IPMeasurementEntry::measuredSample)
        .toList();
  }

  private void emitUpdatedEvent() {
    var measurementUpdatedEvent = new MeasurementUpdatedEvent(projectId().value(),
        this.measurementId().value());
    LocalDomainEventDispatcher.instance().dispatch(measurementUpdatedEvent);
  }

  private void emitCreatedEvent() {
    var measurementCreatedEvent = new MeasurementCreatedEvent(this.measurementId());
    LocalDomainEventDispatcher.instance().dispatch(measurementCreatedEvent);
  }

  public String measurementName() {
    return Optional.ofNullable(this.measurementName).orElse("");
  }

  public void setMeasurementName(String measurementName) {
    this.measurementName = Objects.requireNonNull(measurementName);
  }

  @Override
  public String toString() {
    return measurementCode.value();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ImmunopeptidomicsMeasurement that)) {
      return false;
    }

    return Objects.equals(measurementId, that.measurementId);
  }

  @Override
  public int hashCode() {
    return measurementId != null ? measurementId.hashCode() : 0;
  }
}
