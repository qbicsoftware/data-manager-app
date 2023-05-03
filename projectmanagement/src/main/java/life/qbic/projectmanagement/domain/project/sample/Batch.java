package life.qbic.projectmanagement.domain.project.sample;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import java.util.*;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;

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

    public void addSample(SampleId sampleId) {
        this.sampleIds.add(sampleId);
    }

    public void removeSample(SampleId sampleToRemove) {
        this.sampleIds.remove(sampleToRemove);
    }

    public BatchId batchId() {
        return this.id;
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
