package life.qbic.projectmanagement.domain.project.sample;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

import java.util.Objects;
import java.util.Optional;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
            SampleId id, BatchId assignedBatch, String label, ExperimentId experimentId, Long experimentalGroupId, SampleOrigin sampleOrigin
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

    public static Sample create(
            String label, BatchId assignedBatch, ExperimentId experimentId, Long experimentalGroupId,
            BiologicalReplicateId replicateReference, SampleOrigin sampleOrigin
    ) {
        SampleId sampleId = SampleId.create();
        return new Sample(sampleId, assignedBatch, label, experimentId, experimentalGroupId, sampleOrigin, replicateReference);
    }

    public BatchId assignedBatch() {
        return this.assignedBatch;
    }

    public SampleId sampleId() {
        return this.id;
    }
}
