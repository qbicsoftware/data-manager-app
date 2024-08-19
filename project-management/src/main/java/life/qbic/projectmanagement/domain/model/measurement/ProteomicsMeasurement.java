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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode.MeasurementCodeConverter;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementCreatedEvent;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementUpdatedEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Proteomics measurement</b>
 * <p>
 * Proteomics implementation for the {@link MeasurementMetadata} interface, our aggregate for
 * proteomics measurement metadata.
 *
 * @since 1.0.0
 */
@Entity(name = "proteomics_measurement")
public class ProteomicsMeasurement {

  @Embedded
  @Column(nullable = false)
  ProjectId projectId;

  @Column(name = "labelType")
  private String labelType = "";

  @Column(name = "lcmsMethod")
  private String lcmsMethod = "";

  @Embedded
  private Organisation organisation;

  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "measurement_id"))
  private MeasurementId measurementId;

  @Column(name = "instrument", columnDefinition = "longtext CHECK (json_valid(`instrument`))")
  private OntologyTerm instrument;

  @Column(name = "samplePool")
  private String samplePool = "";

  @Column(name = "measurementCode")
  @Convert(converter = MeasurementCodeConverter.class)
  private MeasurementCode measurementCode;

  @Column(name = "registration")
  private Instant registration;

  @Column(name = "facility")
  private String facility = "";

  @Column(name = "digestionMethod")
  private String digestionMethod = "";

  @Column(name = "digestionEnzyme")
  private String digestionEnzyme = "";

  @Column(name = "enrichmentMethod")
  private String enrichmentMethod = "";

  @Column(name = "injectionVolume")
  private int injectionVolume;

  @Column(name = "lcColumn")
  private String lcColumn = "";

  @ElementCollection(targetClass = ProteomicsSpecificMeasurementMetadata.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "specific_measurement_metadata_pxp", joinColumns = @JoinColumn(name = "measurement_id"))
  private Set<ProteomicsSpecificMeasurementMetadata> specificMetadata;

  protected ProteomicsMeasurement() {
    // Needed for JPA
  }

  private ProteomicsMeasurement(ProjectId projectId, MeasurementId id,
      MeasurementCode measurementCode,
      Organisation organisation, ProteomicsMethodMetadata method, Instant registration,
      Collection<ProteomicsSpecificMeasurementMetadata> proteomicsMeasurementMetadata) {
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    evaluateMandatoryMetadata(
        method); // throws IllegalArgumentException if required properties are missing
    evaluateMandatorySpecificMetadata(
        proteomicsMeasurementMetadata); // throws IllegalArgumentException if required properties are missing
    this.measurementId = id;
    this.organisation = organisation;
    this.measurementCode = measurementCode;
    this.registration = registration;
    setMethodMetadata(method);
    this.specificMetadata = new HashSet<>(proteomicsMeasurementMetadata);
    emitCreatedEvent();
  }

  private static void evaluateMandatorySpecificMetadata(
      Collection<ProteomicsSpecificMeasurementMetadata> metadata) throws IllegalArgumentException {
    metadata.forEach(ProteomicsMeasurement::evaluateMandatorySpecificMetadata);
  }

  private static void evaluateMandatorySpecificMetadata(
      ProteomicsSpecificMeasurementMetadata metadata) throws IllegalArgumentException {
    if (metadata.measuredSample() == null) {
      throw new IllegalArgumentException("Measured Sample: Missing metadata");
    }
  }

  private static void evaluateMandatoryMetadata(ProteomicsMethodMetadata method)
      throws IllegalArgumentException {
    if (method.instrument() == null) {
      throw new IllegalArgumentException("Instrument: Missing metadata.");
    }
    if (method.facility().isBlank()) {
      throw new IllegalArgumentException("Facility: Missing metadata");
    }
    if (method.digestionMethod().isBlank()) {
      throw new IllegalArgumentException("Digestion Method: Missing metadata");
    }
    if (method.digestionEnzyme().isBlank()) {
      throw new IllegalArgumentException("Digestion Enzyme: Missing metadata");
    }
    if (method.lcColumn().isBlank()) {
      throw new IllegalArgumentException("LC column: Missing metadata");
    }
  }

  /**
   * Creates a new {@link ProteomicsMeasurement} object instance, that describes an NGS measurement
   * entity with many describing properties about provenance and instrumentation.
   *
   * @param projectId
   * @return
   * @throws IllegalArgumentException in case there are missing required metadata.
   * @since 1.0.0
   */
  public static ProteomicsMeasurement create(ProjectId projectId,
      MeasurementCode measurementCode, Organisation organisation, ProteomicsMethodMetadata method,
      Collection<ProteomicsSpecificMeasurementMetadata> proteomicsSpecificMeasurementMetadata)
      throws IllegalArgumentException {
    requireNonNull(method.instrument());
    requireNonNull(measurementCode);
    requireNonNull(proteomicsSpecificMeasurementMetadata);
    if (!measurementCode.isMSDomain()) {
      throw new IllegalArgumentException(
          "Proteomics code is not from the Proteomics domain for: \"" + measurementCode + "\"");
    }
    if (proteomicsSpecificMeasurementMetadata.isEmpty()) {
      throw new IllegalArgumentException(
          "No specific metadata provided: Specific metadata must contain at least one entry with a measured sample reference."
      );
    }
    var measurementId = MeasurementId.create();
    return new ProteomicsMeasurement(projectId, measurementId, measurementCode,
        organisation,
        method, Instant.now(), proteomicsSpecificMeasurementMetadata);
  }

  public void setSpecificMetadata(
      Collection<ProteomicsSpecificMeasurementMetadata> specificMetadata) {
    this.specificMetadata = new HashSet<>(specificMetadata);
  }

  public Collection<ProteomicsSpecificMeasurementMetadata> specificMetadata() {
    return new HashSet<>(specificMetadata);
  }

  public void addSpecificMetadata(ProteomicsSpecificMeasurementMetadata specificMetadata) {
    this.specificMetadata.add(specificMetadata);
  }

  /**
   * Convenience method to search if the measurement was derived from a pooled sample.
   *
   * @return true, if the measurement was performed on a pooled sample, else returns false
   * @since 1.0.0
   */
  public boolean isPooledSampleMeasurement() {
    return specificMetadata.size() > 1;
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

  public Collection<SampleId> measuredSamples() {
    return specificMetadata.stream().map(ProteomicsSpecificMeasurementMetadata::measuredSample)
        .toList();
  }

  public OntologyTerm instrument() {
    return instrument;
  }

  public Organisation organisation() {
    return organisation;
  }

  public String facility() {
    return facility;
  }

  public String digestionMethod() {
    return digestionMethod;
  }

  public String digestionEnzyme() {
    return digestionEnzyme;
  }

  public String enrichmentMethod() {
    return enrichmentMethod;
  }

  public String lcColumn() {
    return lcColumn;
  }

  public String lcmsMethod() {
    return lcmsMethod;
  }

  public Instant registrationDate() {
    return registration;
  }
  
  public void updateMethod(ProteomicsMethodMetadata method) {
    setMethodMetadata(method);
    emitUpdatedEvent();
  }

  private void setMethodMetadata(ProteomicsMethodMetadata methodMetadata) {
    this.instrument = methodMetadata.instrument();
    this.facility = methodMetadata.facility();
    this.digestionMethod = methodMetadata.digestionMethod();
    this.digestionEnzyme = methodMetadata.digestionEnzyme();
    this.enrichmentMethod = methodMetadata.enrichmentMethod();
    this.lcColumn = methodMetadata.lcColumn();
    this.lcmsMethod = methodMetadata.lcmsMethod();
    this.labelType = methodMetadata.labelType();
    this.injectionVolume = methodMetadata.injectionVolume();
  }

  private void emitUpdatedEvent() {
    var measurementUpdatedEvent = new MeasurementUpdatedEvent(this.measurementId());
    LocalDomainEventDispatcher.instance().dispatch(measurementUpdatedEvent);
  }

  private void emitCreatedEvent() {
    var measurementCreatedEvent = new MeasurementCreatedEvent(this.measurementId());
    LocalDomainEventDispatcher.instance().dispatch(measurementCreatedEvent);
  }

  public void setSamplePoolGroup(String group) {
    this.samplePool = group;
  }

  public Optional<String> samplePoolGroup() {
    return samplePool.isBlank() ? Optional.empty() : Optional.of(samplePool);
  }

  public int injectionVolume() {
    return injectionVolume;
  }

  public String labelType() {
    return labelType;
  }

  public void setOrganisation(Organisation organisation) {
    this.organisation = organisation;
    emitUpdatedEvent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProteomicsMeasurement that)) {
      return false;
    }

    return Objects.equals(measurementId, that.measurementId);
  }

  @Override
  public int hashCode() {
    return measurementId != null ? measurementId.hashCode() : 0;
  }

  public Optional<String> comment() {
    return Optional.empty();
  }
}
