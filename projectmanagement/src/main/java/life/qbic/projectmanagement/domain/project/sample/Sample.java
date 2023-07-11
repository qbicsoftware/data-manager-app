package life.qbic.projectmanagement.domain.project.sample;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

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
  @AttributeOverride(name = "id", column = @Column(name = "bio_replicate_id"))
  private BiologicalReplicateId biologicalReplicateId;
  @Embedded
  @AttributeOverride(name = "uuid", column = @Column(name = "experiment_id"))
  private ExperimentId experimentId;
  private Long experimentalGroupId;
  @EmbeddedId
  @AttributeOverride(name = "uuid", column = @Column(name = "sample_id"))
  private SampleId id;
  private String label;
  private String comment;
  @Column(name = "analysis_type")
  private String analysisType;
  @Embedded
  private SampleCode sampleCode;
  @Embedded
  private SampleOrigin sampleOrigin;

  private Sample(SampleId id, SampleCode sampleCode, BatchId assignedBatch, String label,
      ExperimentId experimentId, Long experimentalGroupId, SampleOrigin sampleOrigin,
      BiologicalReplicateId replicateReference, String analysisType, String comment
  ) {
    this.id = id;
    this.sampleCode = Objects.requireNonNull(sampleCode);
    this.label = label;
    this.experimentId = experimentId;
    this.experimentalGroupId = experimentalGroupId;
    this.sampleOrigin = sampleOrigin;
    this.biologicalReplicateId = replicateReference;
    this.assignedBatch = assignedBatch;
    this.comment = comment;
    this.analysisType = analysisType;
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
  public static Sample create(
      SampleCode sampleCode,
      SampleRegistrationRequest sampleRegistrationRequest) {
    Objects.requireNonNull(sampleRegistrationRequest);
    SampleId sampleId = SampleId.create();
    return new Sample(sampleId, sampleCode,
        sampleRegistrationRequest.assignedBatch(),
        sampleRegistrationRequest.label(), sampleRegistrationRequest.experimentId(),
        sampleRegistrationRequest.experimentalGroupId(),
        sampleRegistrationRequest.sampleOrigin(), sampleRegistrationRequest.replicateReference(),
        sampleRegistrationRequest.analysisType(), sampleRegistrationRequest.comment());
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

  public Long experimentalGroupId() {
    return this.experimentalGroupId;
  }

  public BiologicalReplicateId biologicalReplicateId() {
    return this.biologicalReplicateId;
  }

}
