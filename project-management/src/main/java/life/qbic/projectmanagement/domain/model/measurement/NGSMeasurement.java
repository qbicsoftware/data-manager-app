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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
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
  @Column(name = "indexI7")
  String indexI7 = "";
  @Column(name = "indexI5")
  String indexI5 = "";
  @Column(name = "comment")
  String comment = "";
  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "measurement_id"))
  private MeasurementId id;
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
  @ElementCollection(targetClass = SampleId.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "ngs_measurement_samples", joinColumns = @JoinColumn(name = "measurement_id"))
  private Collection<SampleId> measuredSamples;

  protected NGSMeasurement() {
    // Needed for JPA
  }

  private NGSMeasurement(MeasurementId measurementId, ProjectId projectId,
      Collection<SampleId> sampleIds,
      MeasurementCode measurementCode,
      Organisation organisation, NGSMethodMetadata method, String comment, Instant registration) {
    evaluateMandatoryMetadata(
        method); // throws IllegalArgumentException if required properties are missing
    measuredSamples = new ArrayList<>();
    measuredSamples.addAll(sampleIds);
    this.id = measurementId;
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    this.organisation = requireNonNull(organisation, "organisation must not be null");
    this.instrument = requireNonNull(method.instrument(), "instrument must not be null");
    this.measurementCode = requireNonNull(measurementCode, "measurement code must not be null");
    this.facility = requireNonNull(method.facility(), "facility must not be null");
    this.sequencingReadType = requireNonNull(method.sequencingReadType(),
        "read type must not be null");
    this.libraryKit = method.libraryKit();
    this.flowCell = method.flowCell();
    this.sequencingRunProtocol = method.sequencingRunProtocol();
    this.indexI7 = method.indexI7();
    this.indexI5 = method.indexI5();
    this.registration = registration;
    this.comment = comment;
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
   * Creates a new {@link NGSMeasurement} object instance, that describes an NGS measurement entity
   * with many describing properties about provenance and instrumentation.
   *
   * @param projectId
   * @param sampleIds the sample ids of the samples the measurement was performed on. If more than
   *                  one sample id is provided, the measurement is considered to be performed on a
   *                  pooled sample
   * @return
   * @since 1.0.0
   */
  public static NGSMeasurement create(ProjectId projectId, Collection<SampleId> sampleIds,
      MeasurementCode measurementCode, Organisation organisation, NGSMethodMetadata method,
      String comment) throws IllegalArgumentException {
    if (sampleIds.isEmpty()) {
      throw new IllegalArgumentException(
          "No sample ids provided. At least one sample id must provided for a measurement.");
    }
    requireNonNull(measurementCode, "measurement code must not be null");
    requireNonNull(method, "method must not be null");
    requireNonNull(method.instrument(), "instrument must not be null");
    if (!measurementCode.isNGSDomain()) {
      throw new IllegalArgumentException(
          "NGSMeasurementMetadata code is not from the NGS domain for: \"" + measurementCode
              + "\"");
    }
    var measurementId = MeasurementId.create();
    return new NGSMeasurement(measurementId, projectId, sampleIds, measurementCode, organisation,
        method,
        comment, Instant.now());
  }

  public void setMethod(NGSMethodMetadata methodMetadata) {
    this.instrument = methodMetadata.instrument();
    this.facility = methodMetadata.facility();
    this.sequencingReadType = methodMetadata.sequencingReadType();
    this.libraryKit = methodMetadata.libraryKit();
    this.flowCell = methodMetadata.flowCell();
    this.sequencingRunProtocol = methodMetadata.sequencingRunProtocol();
    this.indexI7 = methodMetadata.indexI7();
    this.indexI5 = methodMetadata.indexI5();
    emitUpdatedEvent();
  }

  public void setComment(String comment) {
    this.comment = comment;
    emitUpdatedEvent();
  }

  public MeasurementCode measurementCode() {
    return this.measurementCode;
  }

  public MeasurementId measurementId() {
    return id;
  }
  public ProjectId projectId() {
    return projectId;
  }

  public Collection<SampleId> measuredSamples() {
    return measuredSamples.stream().toList();
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

  public Optional<String> indexI7() {
    return Optional.ofNullable(indexI7.isBlank() ? null : indexI7);
  }

  public Optional<String> indexI5() {
    return Optional.ofNullable(indexI5.isBlank() ? null : indexI5);
  }

  public Optional<String> comment() {
    return Optional.ofNullable(comment.isBlank() ? null : comment);
  }

  private void emitUpdatedEvent() {
    var measurementUpdatedEvent = new MeasurementUpdatedEvent(this.measurementId());
    LocalDomainEventDispatcher.instance().dispatch(measurementUpdatedEvent);
  }

  private void emitCreatedEvent() {
    var measurementCreatedEvent = new MeasurementCreatedEvent(this.measurementId());
    LocalDomainEventDispatcher.instance().dispatch(measurementCreatedEvent);
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

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
