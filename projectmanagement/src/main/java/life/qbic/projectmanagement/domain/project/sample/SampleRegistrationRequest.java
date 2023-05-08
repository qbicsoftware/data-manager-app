package life.qbic.projectmanagement.domain.project.sample;

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
 * @since 1.0.0
 */
public record SampleRegistrationRequest(String label, BatchId assignedBatch,
                                        ExperimentId experimentId, Long experimentalGroupId,
                                        BiologicalReplicateId replicateReference,
                                        SampleOrigin sampleOrigin) {

}
