package life.qbic.projectmanagement.application.measurement.foobar.jpa;

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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * A simple JPA object directing
 */
@Entity(name = "NgsMeasurement")
@Table(name = "ngs_measurements")
public class NGSMeasurement {

  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "measurement_id"))
  private MeasurementId measurementId;
  @Convert(converter = MeasurementCode.MeasurementCodeConverter.class)
  @Column(name = "measurementCode")
  private MeasurementCode measurementCode;

  @Embedded
  @AttributeOverride(name = "projectId", column = @Column(name = "projectId"))
  @Column(nullable = false)
  private ProjectId projectId;

  @Column(name = "facility")
  private String facility = "";
  @Column(name = "readType")
  private String sequencingReadType = "";
  @Column(name = "libraryKit")
  private String libraryKit = "";
  @Column(name = "flowcell")
  private String flowCell = "";
  @Column(name = "runProtocol")
  private String sequencingRunProtocol = "";

  @Column(name = "instrument", columnDefinition = "longtext CHECK (json_valid(`instrument`))")
  private OntologyTerm instrument;

  @Column(name = "registrationTime")
  private Instant registration;

  @Column(name = "samplePool")
  private String samplePool;

  @Embedded
  @AttributeOverride(name = "IRI", column = @Column(name = "IRI"))
  @AttributeOverride(name = "label", column = @Column(name = "label"))
  private Organisation organisation;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "specific_measurement_metadata_ngs", joinColumns = @JoinColumn(name = "measurement_id"))
  @AttributeOverride(name = "sampleId", column = @Column(name = "sample_id"))
  @AttributeOverride(name = "indexI5", column = @Column(name = "indexI5"))
  @AttributeOverride(name = "indexI7", column = @Column(name = "indexI7"))
  @AttributeOverride(name = "comment", column = @Column(name = "comment"))
  private Set<NGSSampleSpecificMetadata> sampleSpecificMetadataSet = new HashSet<>();

  protected NGSMeasurement() {
    //JPA specification
  }

  public NGSMeasurement(MeasurementId measurementId, MeasurementCode measurementCode,
      ProjectId projectId, String facility, String sequencingReadType, String libraryKit,
      String flowCell, String sequencingRunProtocol, OntologyTerm instrument, Instant registration,
      String samplePool, Organisation organisation,
      NGSSampleSpecificMetadata sampleSpecificMetadata) {

    this.measurementId = measurementId;
    this.measurementCode = measurementCode;
    this.projectId = projectId;
    this.facility = facility;
    this.sequencingReadType = sequencingReadType;
    this.libraryKit = libraryKit;
    this.flowCell = flowCell;
    this.sequencingRunProtocol = sequencingRunProtocol;
    this.instrument = instrument;
    this.registration = registration;
    this.samplePool = samplePool;
    this.organisation = organisation;
    this.sampleSpecificMetadataSet = Set.of(sampleSpecificMetadata);
  }

  public MeasurementId getMeasurementId() {
    return measurementId;
  }

  public MeasurementCode getMeasurementCode() {
    return measurementCode;
  }

  public ProjectId getProjectId() {
    return projectId;
  }

  public String getFacility() {
    return facility;
  }

  public String getSequencingReadType() {
    return sequencingReadType;
  }

  public OntologyTerm getInstrument() {
    return instrument;
  }

  public Instant getRegistration() {
    return registration;
  }

  public String samplePool() {
    return samplePool;
  }

  public boolean isPooled() {
    return samplePool != null && !samplePool.isEmpty();
  }

  public Organisation getOrganisation() {
    return organisation;
  }

  public Set<NGSSampleSpecificMetadata> getSampleSpecificMetadata() {
    return Set.copyOf(sampleSpecificMetadataSet);
  }

  public Optional<String> getLibraryKit() {
    return Optional.ofNullable(libraryKit);
  }

  public Optional<String> getFlowCell() {
    return Optional.ofNullable(flowCell);
  }

  public Optional<String> getSequencingRunProtocol() {
    return Optional.ofNullable(sequencingRunProtocol);
  }
}
