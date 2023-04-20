package life.qbic.projectmanagement.domain.project.sample;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.util.*;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Entity(name = "sample_batches")
public class Batch {

    @EmbeddedId
    private BatchId id;

    @Column(name = "batchLabel")
    private String label;

    @Column(name = "isPilot")
    private boolean pilot;

    @ElementCollection(targetClass = SampleId.class)
    private List<SampleId> sampleIds;

    protected Batch() {

    }

    private Batch(BatchId id, String label, Collection<SampleId> sampleIds, boolean isPilot) {
        this.id = id;
        this.label = label;
        this.sampleIds = sampleIds.stream().toList();
        this.pilot = isPilot;
    }

    public static Batch create(String label) {
        return create(label, false);
    }

    public static Batch create(String label, boolean isPilot) {
        return create(label, new ArrayList<>(), isPilot);
    }

    public static Batch create(String label, Collection<SampleId> sampleIds, boolean isPilot) {
        BatchId batchId = BatchId.create();
        return new Batch(batchId, label, sampleIds.stream().toList(), isPilot);
    }

    public AddSampleResponse addSample(Sample sample) {
        if (sample.assignedBatch().isPresent()) {
            return new AddSampleResponse(AddSampleResponse.ResponseCode.SAMPLE_ALREADY_IN_OTHER_BATCH);
        }
        this.sampleIds.add(sample.sampleId());
        sample.assignToBatch(this);
        return new AddSampleResponse(AddSampleResponse.ResponseCode.SUCCESS);
    }

    public RemoveSampleResponse removeSample(Sample sampleToRemove) {
        Optional<SampleId> sampleInBatch =
                sampleIds.stream().filter(sampleId -> sampleToRemove.sampleId().equals(sampleId)).findAny();
        if (sampleInBatch.isEmpty()) {
            return new RemoveSampleResponse(RemoveSampleResponse.ResponseCode.SAMPLE_NOT_IN_BATCH);
        }
        sampleIds.remove(sampleToRemove.sampleId());
        return new RemoveSampleResponse(RemoveSampleResponse.ResponseCode.SUCCESS);
    }

    public BatchId batchId() {
        return this.id;
    }

    public record AddSampleResponse(ResponseCode responseCode) {
        public enum ResponseCode {
            SUCCESS, SAMPLE_ALREADY_IN_OTHER_BATCH
        }
    }

    public record RemoveSampleResponse(ResponseCode responseCode) {
        public enum ResponseCode {
            SUCCESS, SAMPLE_NOT_IN_BATCH
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Batch batch = (Batch) o;
        return Objects.equals(id, batch.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
