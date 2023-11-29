package life.qbic.projectmanagement.domain.model.batch;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Batch Entity</b>
 * <p>
 * A batch forms a logical container of samples that are going to be shipped together to the
 * measurement facility to get analysed.
 *
 * @since 1.0.0
 */
@Entity(name = "sample_batches")
public class Batch {

  @EmbeddedId
  @Column(name = "id")
  private BatchId id;

  @Column(name = "batchLabel")
  private String label;

  @Column(name = "isPilot")
  private boolean pilot;

  @ElementCollection(targetClass = SampleId.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "sample_batches_sampleid", joinColumns = @JoinColumn(name = "batch_id"))
  private List<SampleId> sampleIds;

  protected Batch() {

  }

  private Batch(BatchId id, String label, Collection<SampleId> sampleIds, boolean isPilot) {
    this.id = Objects.requireNonNull(id);
    this.label = Objects.requireNonNull(label);
    this.sampleIds = Objects.requireNonNull(sampleIds.stream().toList());
    this.pilot = isPilot;
  }

  /**
   * Creates an empty batch.
   * <p>
   * The batch is NOT marked as a pilot batch and considered as a standard batch.
   *
   * @param label a human-readable semantic label of the batch
   * @return the created batch
   * @since 1.0.0
   */
  public static Batch create(String label) {
    return create(label, false);
  }

  /**
   * Creates an empty batch.
   * <p>
   * The batch can be marked as a pilot batch if required.
   *
   * @param label   a human-readable semantic label of the batch
   * @param isPilot true, if the batch is a pilot batch
   * @return the created batch
   * @since 1.0.0
   */
  public static Batch create(String label, boolean isPilot) {
    return new Batch(BatchId.create(), label, new ArrayList<>(), isPilot);
  }

  public void addSample(SampleId sampleId) {
    this.sampleIds.add(sampleId);
  }

  public void removeSample(SampleId sampleToRemove) {
    this.sampleIds.remove(sampleToRemove);
  }

  public void setLabel(String label) {
    this.label = label;
  }
  public void setPilot(boolean pilot) {
    this.pilot = pilot;
  }


  public BatchId batchId() {
    return this.id;
  }

  public String label() {
    return label;
  }

  public boolean isPilot() {
    return pilot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Batch batch = (Batch) o;
    return Objects.equals(id, batch.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
