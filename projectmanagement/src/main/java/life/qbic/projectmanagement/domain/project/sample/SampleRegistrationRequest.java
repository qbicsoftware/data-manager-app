package life.qbic.projectmanagement.domain.project.sample;

import java.util.Objects;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * Sample registration request.
 * <p>
 * Serves as a parameter object for sample creation.
 *
 * @param label               a human-readable semantic descriptor of the sample
 * @param assignedBatch       the assigned batch
 * @param experimentId        the experiment reference
 * @param experimentalGroupId the experimental group id the sample is part of
 * @param replicateReference  the biological replicated reference the sample has been taken from
 * @param sampleOrigin        information about the sample origin.
 * @param analysisType        analysis to be performed
 * @param comment             comment relating to the sample
 * @since 1.0.0
 */
public record SampleRegistrationRequest(String label, BatchId assignedBatch,
                                        ExperimentId experimentId, Long experimentalGroupId,
                                        BiologicalReplicateId replicateReference,
                                        SampleOrigin sampleOrigin, String analysisType, String comment) {

  public SampleRegistrationRequest(String label, BatchId assignedBatch, ExperimentId experimentId,
      Long experimentalGroupId, BiologicalReplicateId replicateReference,
      SampleOrigin sampleOrigin, String analysisType, String comment) {
    this.label = Objects.requireNonNull(label);
    this.assignedBatch = Objects.requireNonNull(assignedBatch);
    this.experimentId = Objects.requireNonNull(experimentId);
    this.experimentalGroupId = Objects.requireNonNull(experimentalGroupId);
    this.replicateReference = Objects.requireNonNull(replicateReference);
    this.sampleOrigin = Objects.requireNonNull(sampleOrigin);
    this.comment = comment;
    this.analysisType = analysisType;
  }
}
