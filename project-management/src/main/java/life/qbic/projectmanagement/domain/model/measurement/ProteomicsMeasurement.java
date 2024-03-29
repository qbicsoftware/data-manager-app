package life.qbic.projectmanagement.domain.model.measurement;

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
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode.MeasurementCodeConverter;
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
public class ProteomicsMeasurement implements MeasurementMetadata {

  @Column(name = "lcmsMethod")
  private String lcmsMethod = "";
  //FIXME do not implement MeasurementMetadata, you are not metadata but the measurement
  @Embedded
  private Organisation organisation;
  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "measurement_id"))
  private MeasurementId id;

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
  private int injectionVolume = 0;

  @Column(name = "lcColumn")
  private String lcColumn = "";
  @Column(name = "comment")
  private String comment = "";

  @Column(name = "measurementLabelingType")
  private String labelingType = "";

  @Column(name = "measurementLabel")
  private String label = "";

  @Column(name = "fraction")
  private String fraction = "";

  @ElementCollection(targetClass = SampleId.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "measurement_samples", joinColumns = @JoinColumn(name = "measurement_id"))
  private Collection<SampleId> measuredSamples;

  protected ProteomicsMeasurement() {
    // Needed for JPA
  }

  private ProteomicsMeasurement(MeasurementId id, Collection<SampleId> sampleIds,
      MeasurementCode measurementCode,
      Organisation organisation, ProteomicsMethodMetadata method, Instant registration) {
    evaluateMandatorMetadata(
        method); // throws IllegalArgumentException if required properties are missing
    measuredSamples = new ArrayList<>();
    measuredSamples.addAll(sampleIds);
    this.id = id;
    this.organisation = organisation;
    this.instrument = method.instrument();
    this.measurementCode = measurementCode;
    this.facility = method.facility();
    this.digestionMethod = method.digestionMethod();
    this.digestionEnzyme = method.digestionEnzyme();
    this.enrichmentMethod = method.enrichmentMethod();
    this.injectionVolume = method.injectionVolume();
    this.lcColumn = method.lcColumn();
    this.lcmsMethod = method.lcmsMethod();
    this.registration = registration;
  }

  private static void evaluateMandatorMetadata(ProteomicsMethodMetadata method)
      throws IllegalArgumentException {
    if (method.instrument() == null) {
      throw new IllegalArgumentException("Instrument: Missing metadata.");
    }
    if (method.facility().isBlank()) {
      throw new IllegalArgumentException("Facility: Missing metadata");
    }
    if (method.fractionName().isBlank()) {
      throw new IllegalArgumentException("Cycle/Fraction Name: Missing metadata");
    }
    if (method.digestionMethod().isBlank()) {
      throw new IllegalArgumentException("Digestion Method: Missing metadata");
    }
    if (method.digestionEnzyme().isBlank()) {
      throw new IllegalArgumentException("Digestion Enzyme: Missing metadata");
    }
    if (method.enrichmentMethod().isBlank()) {
      throw new IllegalArgumentException("Enrichment Method: Missing metadata");
    }
    if (method.injectionVolume() <= 0) {
      throw new IllegalArgumentException(
          "Injection Volume: smaller / equal to zero: %s".formatted(method.injectionVolume()));
    }
    if (method.lcColumn().isBlank()) {
      throw new IllegalArgumentException("LC column: Missing metadata");
    }
    if (method.lcmsMethod().isBlank()) {
      throw new IllegalArgumentException("LCMS method: Missing metadata");
    }
  }

  /**
   * Creates a new {@link ProteomicsMeasurement} object instance, that describes an NGS measurement
   * entity with many describing properties about provenance and instrumentation.
   *
   * @param sampleIds the sample ids of the samples the measurement was performed on. If more than
   *                  one sample id is provided, the measurement is considered to be performed on a
   *                  pooled sample
   * @return
   * @throws IllegalArgumentException in case there are missing required metadata.
   * @since 1.0.0
   */
  public static ProteomicsMeasurement create(Collection<SampleId> sampleIds,
      MeasurementCode measurementCode, Organisation organisation, ProteomicsMethodMetadata method)
      throws IllegalArgumentException {
    if (sampleIds.isEmpty()) {
      throw new IllegalArgumentException(
          "No sample ids provided. At least one sample id must provided for a measurement.");
    }
    Objects.requireNonNull(method.instrument());
    Objects.requireNonNull(measurementCode);
    if (!measurementCode.isMSDomain()) {
      throw new IllegalArgumentException(
          "Proteomics code is not from the Proteomics domain for: \"" + measurementCode + "\"");
    }
    var measurementId = MeasurementId.create();
    return new ProteomicsMeasurement(measurementId, sampleIds, measurementCode, organisation,
        method, Instant.now());
  }

  public static ProteomicsMeasurement create(Collection<SampleId> sampleIds, MeasurementCode code,
      Organisation organisation, ProteomicsMethodMetadata method,
      ProteomicsSamplePreparation samplePreparation) {
    var measurement = create(sampleIds, code, organisation, method);
    measurement.setSamplePreparation(samplePreparation);
    return measurement;
  }

  public void setSamplePreparation(ProteomicsSamplePreparation samplePreparation) {
    this.comment = samplePreparation.comment();
  }

  public void setLabeling(ProteomicsLabeling labeling) {
    this.labelingType = labeling.labelType();
    this.label = labeling.label();
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

  public int injectionVolume() {
    return injectionVolume;
  }

  public String lcColumn() {
    return lcColumn;
  }

  public String lcmsMethod() {
    return lcmsMethod;
  }

  public Optional<String> comment() {
    return Optional.ofNullable(comment.isBlank() ? null : comment);
  }

  public Optional<String> labelingType() {
    return Optional.ofNullable(labelingType.isBlank() ? null : labelingType);
  }

  public Optional<String> label() {
    return Optional.ofNullable(label.isBlank() ? null : label);
  }

  public Instant registrationDate() {
    return registration;
  }


  public void setSamplePoolGroup(String group) {
    this.samplePool = group;
  }

  public Optional<String> samplePoolGroup() {
    return samplePool.isBlank() ? Optional.empty() : Optional.of(samplePool);
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
