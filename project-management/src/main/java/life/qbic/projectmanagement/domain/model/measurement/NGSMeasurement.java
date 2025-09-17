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
 * <b>NGS Metadata Object</b>
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

  @Column(name = "facility")
  String facility = "";
  @Column(name = "readType")
  String sequencingReadType = "";
  @Column(name = "libraryKit")
  String libraryKit = "";
  @Column(name = "flowcell")
  String flowCell = "";
  @Column(name = "runProtocol")
  String sequencingRunProtocol = "";
  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "measurement_id"))
  private MeasurementId measurementId;
  @Embedded
  @Column(nullable = false)
  private ProjectId projectId;
  @Column(name = "instrument", columnDefinition = "longtext CHECK (json_valid(`instrument`))")
  private OntologyTerm instrument;
  @Column(name = "samplePool")
  private String samplePool = "";
  @Convert(converter = MeasurementCode.MeasurementCodeConverter.class)
  private MeasurementCode measurementCode;
  @Column(name = "registrationTime")
  private Instant registration;
  @Embedded
  private Organisation organisation;

  @Column(name = "measurementName")
  private String measurementName;

  @ElementCollection(targetClass = NGSSpecificMeasurementMetadata.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "specific_measurement_metadata_ngs", joinColumns = @JoinColumn(name = "measurement_id"))
  private Set<NGSSpecificMeasurementMetadata> specificMetadata = new HashSet<>();

  protected NGSMeasurement() {
    // Needed for JPA
  }

  private NGSMeasurement(MeasurementId measurementId, ProjectId projectId,
      String samplePool,
      MeasurementCode measurementCode,
      Organisation organisation, NGSMethodMetadata method, Instant registration,
      Collection<NGSSpecificMeasurementMetadata> measurementMetadata) {
    if (!measurementCode.isNGSDomain()) {
      throw new IllegalArgumentException(
          "NGSMeasurementMetadata code is not from the NGS domain for: \"" + measurementCode
              + "\"");
    }
    evaluateMandatoryMetadata(
        method); // throws IllegalArgumentException if required properties are missing
    this.measurementId = measurementId;
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    this.organisation = requireNonNull(organisation, "organisation must not be null");
    this.instrument = requireNonNull(method.instrument(), "msDevice must not be null");
    this.measurementCode = requireNonNull(measurementCode, "measurement code must not be null");
    this.facility = requireNonNull(method.facility(), "facility must not be null");
    this.sequencingReadType = requireNonNull(method.sequencingReadType(),
        "read type must not be null");
    this.libraryKit = method.libraryKit();
    this.flowCell = method.flowCell();
    this.sequencingRunProtocol = method.sequencingRunProtocol();
    this.registration = registration;
    this.samplePool = samplePool;
    this.specificMetadata = new HashSet<>(measurementMetadata);
    emitCreatedEvent();
  }

  private static void evaluateMandatoryMetadata(NGSMethodMetadata method)
      throws IllegalArgumentException {
    if (method.facility().isBlank()) {
      throw new IllegalArgumentException("Facility: Missing metadata");
    }
    if (method.sequencingReadType().isBlank()) {
      throw new IllegalArgumentException("Sequencing Read Type: Missing metadata");
    }
  }

  /**
   * Creates a new pooled {@link NGSMeasurement} object instance, that describes an NGS measurement
   * entity with many describing properties about provenance and instrumentation.
   *
   * @param projectId                   the project id the measurement belongs to
   * @param samplePool                  the sample pool label the measurement represents
   * @param measurementCode             the assigned measurement code for the measurement
   * @param organisation                where the measurement has been conducted
   * @param method                      measurement method related metadata
   * @param specificMeasurementMetadata sample specific metadata
   * @return an instance of an {@link NGSMeasurement}
   * @since 1.0.0
   */
  public static NGSMeasurement createWithPool(ProjectId projectId, String samplePool,
      MeasurementCode measurementCode, Organisation organisation, NGSMethodMetadata method,
      Collection<NGSSpecificMeasurementMetadata> specificMeasurementMetadata)
      throws IllegalArgumentException {
    requireNonNull(measurementCode, "measurement Code must not be null");
    requireNonNull(method, "method must not be null");
    requireNonNull(method.instrument(), "msDevice must not be null");
    if (samplePool.isBlank()) {
      throw new IllegalArgumentException("Sample Pool: no value provided");
    }
    if (specificMeasurementMetadata.stream().map(NGSSpecificMeasurementMetadata::index)
        .anyMatch(Optional::isEmpty)) {
      throw new IllegalArgumentException(
          "All specific metadata must have an index in a pooled measurement");
    }
    var measurementId = MeasurementId.create();
    return new NGSMeasurement(measurementId, projectId, samplePool, measurementCode, organisation,
        method, Instant.now(), specificMeasurementMetadata);
  }

  /**
   * Creates a new single {@link NGSMeasurement} object instance, that describes an NGS measurement
   * entity with many describing properties about provenance and instrumentation.
   *
   * @param projectId                   the project id the measurement belongs to
   * @param measurementCode             the assigned measurement code for the measurement
   * @param organisation                where the measurement has been conducted
   * @param method                      measurement method related metadata
   * @param specificMeasurementMetadata sample specific metadata
   * @return an instance of an {@link NGSMeasurement}
   * @since 1.0.0
   */
  public static NGSMeasurement createSingleMeasurement(ProjectId projectId,
      MeasurementCode measurementCode, Organisation organisation, NGSMethodMetadata method,
      NGSSpecificMeasurementMetadata specificMeasurementMetadata) {
    requireNonNull(measurementCode, "Measurement Code cannot be null");
    requireNonNull(method, "NGsMethodMetadata cannot be null");
    requireNonNull(method.instrument(), "Instrument cannot be null");
    return new NGSMeasurement(MeasurementId.create(), projectId, "", measurementCode, organisation,
        method, Instant.now(), List.of(specificMeasurementMetadata));
  }

  public void setMethod(NGSMethodMetadata methodMetadata) {
    this.instrument = methodMetadata.instrument();
    this.facility = methodMetadata.facility();
    this.sequencingReadType = methodMetadata.sequencingReadType();
    this.libraryKit = methodMetadata.libraryKit();
    this.flowCell = methodMetadata.flowCell();
    this.sequencingRunProtocol = methodMetadata.sequencingRunProtocol();
    emitUpdatedEvent();
  }

  public void setSpecificMetadata(Collection<NGSSpecificMeasurementMetadata> specificMetadata) {
    this.specificMetadata = new HashSet<>(specificMetadata);
    emitUpdatedEvent();
  }

  public Collection<NGSSpecificMeasurementMetadata> specificMeasurementMetadata() {
    return specificMetadata.stream().toList();
  }

  public void setOrganisation(Organisation organisation) {
    this.organisation = Objects.requireNonNull(organisation);
    emitUpdatedEvent();
  }

  public void updateMethod(NGSMethodMetadata methodMetadata) {
    Objects.requireNonNull(methodMetadata);
    setMethod(methodMetadata);
  }

  /**
   * Convenience method to query if the measurement was derived from a single sample.
   *
   * @return true, if the measurement was performed on a single sample, else returns false if the
   * measurement was derived from pooled samples
   * @since 1.0.0
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

  public String sequencingReadType() {
    return sequencingReadType;
  }

  public Optional<String> libraryKit() {
    return Optional.ofNullable(libraryKit.isBlank() ? null : libraryKit);
  }

  public Optional<String> flowCell() {
    return Optional.ofNullable(flowCell.isBlank() ? null : flowCell);
  }

  public Optional<String> sequencingRunProtocol() {
    return Optional.ofNullable(sequencingRunProtocol.isBlank() ? null : sequencingRunProtocol);
  }

  public Collection<SampleId> measuredSamples() {
    return specificMetadata.stream().map(NGSSpecificMeasurementMetadata::measuredSample)
        .toList();
  }

  private void emitUpdatedEvent() {
    var measurementUpdatedEvent = new MeasurementUpdatedEvent(this.measurementId());
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
    this.measurementName = measurementName;
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
    if (!(o instanceof NGSMeasurement that)) {
      return false;
    }

    return Objects.equals(measurementId, that.measurementId);
  }

  @Override
  public int hashCode() {
    return measurementId != null ? measurementId.hashCode() : 0;
  }
}
