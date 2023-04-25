package life.qbic.projectmanagement.domain.project.sample;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
    private BatchId assignedBatch = null;

    private Long experimentalGroupId;

    @Embedded
    private ExperimentId experimentId;

    @Embedded
    private SampleOrigin sampleOrigin;

    @Embedded
    private BiologicalReplicateLabel biologicalReplicateLabel;

    private Sample(
            SampleId id, String label, ExperimentId experimentId, Long experimentalGroupId, SampleOrigin sampleOrigin
            , BiologicalReplicateLabel biologicalReplicateLabel
    ) {
        this.id = id;
        this.label = label;
        this.experimentId = experimentId;
        this.experimentalGroupId = experimentalGroupId;
        this.sampleOrigin = sampleOrigin;
        this.biologicalReplicateLabel = biologicalReplicateLabel;
    }

    protected Sample() {
        // needed for JPA
    }

    public static Sample create(
            String label, ExperimentId experimentId, Long experimentalGroupId,
            BiologicalReplicateLabel biologicalReplicateLabel, SampleOrigin sampleOrigin
    ) {
        SampleId sampleId = SampleId.create();
        return new Sample(sampleId, label, experimentId, experimentalGroupId, sampleOrigin, biologicalReplicateLabel);
    }

    public SampleAddResponse assignToBatch(Batch batch) {
        Objects.requireNonNull(batch, "Batch id must not be null");
        if (assignedBatch().isPresent()) {
            return new SampleAddResponse(SampleAddResponse.ResponseCode.ALREADY_IN_BATCH);
        }
        assignedBatch = batch.batchId();
        return new SampleAddResponse(SampleAddResponse.ResponseCode.SUCCESSFUL);
    }

    public Optional<BatchId> assignedBatch() {
        return Optional.ofNullable(this.assignedBatch);
    }

    public record SampleAddResponse(ResponseCode code) {
        public enum ResponseCode {
            SUCCESSFUL, ALREADY_IN_BATCH
        }
    }

    public SampleId sampleId() {
        return this.id;
    }
}
