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
  @Column(name = "sampleMass")
  Double sampleMass;
  @Column(name = "sampleVolume")
  Double sampleVolume;
  @Column(name = "cycleFractionName")
  String cycleFractionName = "";
  @Column(name = "prepDate")
  java.time.LocalDate prepDate;
  @Column(name = "msRunDate")
  java.time.LocalDate msRunDate;
  @Column(name = "comment")
  String comment = "";
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
    evaluateMandatoryMetadata(method);
    this.measurementId = measurementId;
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    this.organisation = requireNonNull(organisation, "organisation must not be null");
    this.instrument = requireNonNull(method.instrument(), "instrument must not be null");
    this.instrumentName = method.instrumentName() != null ? method.instrumentName() : "";
    this.measurementCode = requireNonNull(measurementCode, "measurement code must not be null");
    this.facility = requireNonNull(method.facility(), "facility must not be null");
    this.mhcAntibody = requireNonNull(method.mhcAntibody(), "mhcAntibody must not be null");
    this.mhcTypingMethod = method.mhcTypingMethod() != null ? method.mhcTypingMethod() : "";
    this.enrichmentMethod = requireNonNull(method.enrichmentMethod(), "enrichmentMethod must not be null");
    this.lcmsMethod = requireNonNull(method.lcmsMethod(), "lcmsMethod must not be null");
    this.lcColumn = requireNonNull(method.lcColumn(), "lcColumn must not be null");
    this.dataAcquisition = requireNonNull(method.dataAcquisition(), "dataAcquisition must not be null");
    this.massRange = requireNonNull(method.massRange(), "massRange must not be null");
    this.retentionTimeRange = method.retentionTimeRange() != null ? method.retentionTimeRange() : 0;
    this.chargeRange = requireNonNull(method.chargeRange(), "chargeRange must not be null");
    this.ionMobilityRange = method.ionMobilityRange() != null ? method.ionMobilityRange() : "";
    this.sampleMass = method.sampleMass();
    this.sampleVolume = method.sampleVolume();
    this.cycleFractionName = method.cycleFractionName() != null ? method.cycleFractionName() : "";
    this.prepDate = method.prepDate();
    this.msRunDate = method.msRunDate();
    this.comment = method.comment() != null ? method.comment() : "";
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
    if (method.mhcAntibody() == null || method.mhcAntibody().isBlank()) {
      throw new IllegalArgumentException("MHC Antibody: Missing metadata");
    }
    if (method.enrichmentMethod() == null || method.enrichmentMethod().isBlank()) {
      throw new IllegalArgumentException("Enrichment method: Missing metadata");
    }
    if (method.lcmsMethod() == null || method.lcmsMethod().isBlank()) {
      throw new IllegalArgumentException("LCMS Method: Missing metadata");
    }
    if (method.lcColumn() == null || method.lcColumn().isBlank()) {
      throw new IllegalArgumentException("LC Column: Missing metadata");
    }
    if (method.dataAcquisition() == null || method.dataAcquisition().isBlank()) {
      throw new IllegalArgumentException("Data Acquisition: Missing metadata");
    }
    if (method.massRange() == null || method.massRange().isBlank()) {
      throw new IllegalArgumentException("Mass range: Missing metadata");
    }
    if (method.chargeRange() == null || method.chargeRange().isBlank()) {
      throw new IllegalArgumentException("Charge range: Missing metadata");
    }
  }

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
    this.mhcAntibody = methodMetadata.mhcAntibody();
    this.mhcTypingMethod = methodMetadata.mhcTypingMethod() != null ? methodMetadata.mhcTypingMethod() : "";
    this.enrichmentMethod = methodMetadata.enrichmentMethod();
    this.lcmsMethod = methodMetadata.lcmsMethod();
    this.lcColumn = methodMetadata.lcColumn();
    this.dataAcquisition = methodMetadata.dataAcquisition();
    this.massRange = methodMetadata.massRange();
    this.retentionTimeRange = methodMetadata.retentionTimeRange() != null ? methodMetadata.retentionTimeRange() : 0;
    this.chargeRange = methodMetadata.chargeRange();
    this.ionMobilityRange = methodMetadata.ionMobilityRange() != null ? methodMetadata.ionMobilityRange() : "";
    this.sampleMass = methodMetadata.sampleMass();
    this.sampleVolume = methodMetadata.sampleVolume();
    this.cycleFractionName = methodMetadata.cycleFractionName() != null ? methodMetadata.cycleFractionName() : "";
    this.prepDate = methodMetadata.prepDate();
    this.msRunDate = methodMetadata.msRunDate();
    this.comment = methodMetadata.comment() != null ? methodMetadata.comment() : "";
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

  public Optional<Double> sampleMass() {
    return Optional.ofNullable(sampleMass);
  }

  public Optional<Double> sampleVolume() {
    return Optional.ofNullable(sampleVolume);
  }

  public Optional<String> cycleFractionName() {
    return cycleFractionName.isBlank() ? Optional.empty() : Optional.of(cycleFractionName);
  }

  public Optional<java.time.LocalDate> prepDate() {
    return Optional.ofNullable(prepDate);
  }

  public Optional<java.time.LocalDate> msRunDate() {
    return Optional.ofNullable(msRunDate);
  }

  public Optional<String> comment() {
    return comment.isBlank() ? Optional.empty() : Optional.of(comment);
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
