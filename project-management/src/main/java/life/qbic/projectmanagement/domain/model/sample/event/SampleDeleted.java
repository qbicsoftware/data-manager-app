package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Sample Deleted - Domain Event</b>
 * <p>
 * A registered sample has been deleted
 *
 * @since 1.0.0
 */
public class SampleDeleted extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 8134640209226646506L;
  @JsonProperty("batchId")
  private final BatchId assignedBatch;
  @JsonProperty("sampleId")
  private final SampleId deletedSample;

  private SampleDeleted(BatchId assignedBatch, SampleId deletedSample) {
    this.assignedBatch = Objects.requireNonNull(assignedBatch);
    this.deletedSample = Objects.requireNonNull(deletedSample);
  }

  /**
   * Creates a new {@link SampleDeleted} object instance.
   *
   * @param assignedBatch    the batch reference the sample will be deleted from
   * @param deletedSample the sample reference of the to be deleted sample
   * @return a new instance of this domain event
   * @since 1.0.0
   */
  public static SampleDeleted create(BatchId assignedBatch, SampleId deletedSample) {
    return new SampleDeleted(assignedBatch, deletedSample);
  }

  @JsonGetter("assignedBatch")
  public BatchId assignedBatch() {
    return this.assignedBatch;
  }

  @JsonGetter("deletedSample")
  public SampleId deletedSample() {
    return this.deletedSample;
  }
}
