package life.qbic.projectmanagement.domain.model.sample;

import java.util.Objects;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
/**
 * Sample registration request.
 * <p>
 * Serves as a parameter object for sample creation.
 *
 * @param label               a human-readable semantic descriptor of the sample
 * @param biologicalReplicate optional identifier of the patient or organism a sample was taken of.
 *                            Used to group biological replicates
 * @param assignedBatch       the assigned batch
 * @param experimentId        the experiment reference
 * @param experimentalGroupId the experimental group id the sample is part of
 * @param sampleOrigin        information about the sample origin.
 * @param analysisMethod      analysis method to be performed
 * @param comment             comment relating to the sample
 *
 * @since 1.0.0
 */
public record SampleRegistrationRequest(String label, String biologicalReplicate, BatchId assignedBatch,
                                        ExperimentId experimentId, Long experimentalGroupId,
                                        SampleOrigin sampleOrigin, AnalysisMethod analysisMethod,
                                        String comment) {

  public SampleRegistrationRequest(String label, String biologicalReplicate, BatchId assignedBatch,
      ExperimentId experimentId, Long experimentalGroupId,
      SampleOrigin sampleOrigin, AnalysisMethod analysisMethod, String comment) {
    this.label = Objects.requireNonNull(label);
    this.biologicalReplicate = biologicalReplicate;
    this.assignedBatch = Objects.requireNonNull(assignedBatch);
    this.experimentId = Objects.requireNonNull(experimentId);
    this.experimentalGroupId = Objects.requireNonNull(experimentalGroupId);
    this.sampleOrigin = Objects.requireNonNull(sampleOrigin);
    this.analysisMethod = Objects.requireNonNull(analysisMethod);
    this.comment = comment;

  }

}
