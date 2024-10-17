package life.qbic.projectmanagement.domain.model.sample;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.util.Objects;
import java.util.Optional;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.projectmanagement.application.batch.SampleUpdateRequest;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered;
import life.qbic.projectmanagement.domain.model.sample.event.SampleUpdated;

/**
 * <b>Sample Entity</b>
 * <p>
 * A sample represents the physical sample from an experiment that has been collected and needs to
 * be prepared for measurement in one of QBiC's partner facilities.
 * <p>
 * A sample needs to be registered and assigned to one existing sample {@link Batch}, before it can
 * be prepared for shipment to the measurement facility.
 *
 * @since 1.0.0
 */
@Entity(name = "sample")
public class Sample {

  @Embedded
  @AttributeOverride(name = "uuid", column = @Column(name = "assigned_batch_id"))
  private BatchId assignedBatch;

  @Embedded
  @AttributeOverride(name = "uuid", column = @Column(name = "experiment_id"))
  private ExperimentId experimentId;
  private Long experimentalGroupId;
  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "sample_id"))
  private SampleId id;
  private String label;
  @Column(name = "organism_id")
  private String biologicalReplicate;
  private String comment;

  @Column(name = "analysis_method")
  @Convert(converter = AnalysisMethodConverter.class)
  private AnalysisMethod analysisMethod;

  @Embedded
  private SampleCode sampleCode;

  @Embedded
  private SampleOrigin sampleOrigin;

  private Sample(SampleId id, SampleCode sampleCode, BatchId assignedBatch, String label,
      String biologicalReplicate, ExperimentId experimentId, Long experimentalGroupId,
      SampleOrigin sampleOrigin,
      AnalysisMethod analysisMethod, String comment) {
    this.id = id;
    this.sampleCode = Objects.requireNonNull(sampleCode);
    this.label = label;
    this.biologicalReplicate = biologicalReplicate;
    this.experimentId = experimentId;
    this.experimentalGroupId = experimentalGroupId;
    this.sampleOrigin = sampleOrigin;
    this.assignedBatch = assignedBatch;
    this.analysisMethod = analysisMethod;
    this.comment = comment;
    emitCreatedEvent();
  }

  protected Sample() {
    // needed for JPA
  }

  /**
   * Creates a new sample entity.
   *
   * @param sampleRegistrationRequest@return the sample
   * @since 1.0.0
   */
  public static Sample create(SampleCode sampleCode,
      SampleRegistrationRequest sampleRegistrationRequest) {
    Objects.requireNonNull(sampleRegistrationRequest);
    SampleId sampleId = SampleId.create();
    return new Sample(sampleId, sampleCode, sampleRegistrationRequest.assignedBatch(),
        sampleRegistrationRequest.label(), sampleRegistrationRequest.biologicalReplicate(),
        sampleRegistrationRequest.experimentId(), sampleRegistrationRequest.experimentalGroupId(),
        sampleRegistrationRequest.sampleOrigin(),
        sampleRegistrationRequest.analysisMethod(), sampleRegistrationRequest.comment());
  }

  public ExperimentId experimentId() {
    return this.experimentId;
  }

  public BatchId assignedBatch() {
    return this.assignedBatch;
  }

  public SampleId sampleId() {
    return this.id;
  }

  public SampleCode sampleCode() {
    return this.sampleCode;
  }

  public SampleOrigin sampleOrigin() {
    return this.sampleOrigin;
  }

  public String label() {
    return this.label;
  }

  public String biologicalReplicate() {
    return this.biologicalReplicate;
  }

  public Optional<String> comment() {
    return Optional.ofNullable(comment);
  }

  public Long experimentalGroupId() {
    return this.experimentalGroupId;
  }

  public AnalysisMethod analysisMethod() {
    return this.analysisMethod;
  }

  public void setAssignedBatch(BatchId assignedBatch) {
    this.assignedBatch = assignedBatch;
  }

  public void setExperimentalGroupId(Long experimentalGroupId) {
    this.experimentalGroupId = experimentalGroupId;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setBiologicalReplicate(String biologicalReplicate) {
    this.biologicalReplicate = biologicalReplicate;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setAnalysisMethod(AnalysisMethod analysisMethod) {
    this.analysisMethod = analysisMethod;
  }

  public void setSampleOrigin(SampleOrigin sampleOrigin) {
    this.sampleOrigin = sampleOrigin;
  }

  public void update(SampleUpdateRequest sampleInfo) {
    setLabel(sampleInfo.sampleInformation().sampleName());
    setBiologicalReplicate(sampleInfo.sampleInformation().biologicalReplicate());
    setAnalysisMethod(sampleInfo.sampleInformation().analysisMethod());
    setSampleOrigin(SampleOrigin.create(sampleInfo.sampleInformation().species(),
        sampleInfo.sampleInformation().specimen(), sampleInfo.sampleInformation().analyte()));
    setComment(sampleInfo.sampleInformation().comment());
    setExperimentalGroupId(sampleInfo.sampleInformation().experimentalGroup().id());
    emitUpdatedEvent();
  }

  static class AnalysisMethodConverter implements AttributeConverter<AnalysisMethod, String> {

    @Override
    public String convertToDatabaseColumn(AnalysisMethod analysisMethod) {
      if (analysisMethod == null) {
        return null;
      }
      return analysisMethod.name();
    }

    @Override
    public AnalysisMethod convertToEntityAttribute(String s) {
      return AnalysisMethod.valueOf(s);
    }
  }

  private void emitUpdatedEvent() {
    var updatedEvent = SampleUpdated.create(this.id);
    LocalDomainEventDispatcher.instance().dispatch(updatedEvent);
  }

  private void emitCreatedEvent() {
    var createdEvent = SampleRegistered.create(this.assignedBatch(), this.id);
    LocalDomainEventDispatcher.instance().dispatch(createdEvent);
  }

}
