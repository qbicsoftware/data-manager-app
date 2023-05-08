package life.qbic.projectmanagement.domain.project.sample;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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

  @EmbeddedId
  private SampleId id;

  private String label;

  @Embedded
  private BatchId assignedBatch;

  private Long experimentalGroupId;

  @Embedded
  private ExperimentId experimentId;

  @Embedded
  private SampleOrigin sampleOrigin;

  @Embedded
  private BiologicalReplicateId biologicalReplicateId;

  private Sample(
      SampleId id, BatchId assignedBatch, String label, ExperimentId experimentId,
      Long experimentalGroupId, SampleOrigin sampleOrigin
      , BiologicalReplicateId replicateReference
  ) {
    this.id = id;
    this.label = label;
    this.experimentId = experimentId;
    this.experimentalGroupId = experimentalGroupId;
    this.sampleOrigin = sampleOrigin;
    this.biologicalReplicateId = replicateReference;
    this.assignedBatch = assignedBatch;
  }

  protected Sample() {
    // needed for JPA
  }

  /**
   * Creates a new sample entity.
   *
   * @param label               a human-readable semantic descriptor of the sample
   * @param assignedBatch       the assigned batch
   * @param experimentId        the experiment reference
   * @param experimentalGroupId the experimental group id the sample is part of
   * @param replicateReference  the biological replicated reference the sample has been taken from
   * @param sampleOrigin        information about the sample origin.
   * @return the sample
   * @since 1.0.0
   */
  public static Sample create(
      String label, BatchId assignedBatch, ExperimentId experimentId, Long experimentalGroupId,
      BiologicalReplicateId replicateReference, SampleOrigin sampleOrigin
  ) {
    SampleId sampleId = SampleId.create();
    return new Sample(sampleId, assignedBatch, label, experimentId, experimentalGroupId,
        sampleOrigin, replicateReference);
  }

  public BatchId assignedBatch() {
    return this.assignedBatch;
  }

  public SampleId sampleId() {
    return this.id;
  }
}
