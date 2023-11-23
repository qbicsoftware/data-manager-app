package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Sample Deleted - Domain Event</b>
 * <p>
 * A previously registered Sample has been deleted
 *
 * @since 1.0.0
 */
public class SampleDeleted extends DomainEvent {

  @Serial
  private static final long serialVersionUID = -1338442721083240618L;
  private final Instant occurredOn;

  @JsonProperty("batchId")
  private final BatchId assignedBatch;

  @JsonProperty("sampleId")
  private final SampleId registeredSample;

  private SampleDeleted(Instant occurredOn, BatchId assignedBatch, SampleId registeredSample) {
    this.occurredOn = Objects.requireNonNull(occurredOn);
    this.assignedBatch = Objects.requireNonNull(assignedBatch);
    this.registeredSample = Objects.requireNonNull(registeredSample);
  }

  /**
   * Creates a new {@link SampleDeleted} object instance.
   *
   * @param assignedBatch    the batch reference the sample was assigned to
   * @param registeredSample the sample reference of the deleted sample
   * @return a new instance of this domain event
   * @since 1.0.0
   */
  public static SampleDeleted create(BatchId assignedBatch, SampleId registeredSample) {
    return new SampleDeleted(Instant.now(), assignedBatch, registeredSample);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return this.occurredOn;
  }

  @JsonGetter("assignedBatch")
  public BatchId assignedBatch() {
    return this.assignedBatch;
  }

  @JsonGetter("registeredSample")
  public SampleId registeredSample() {
    return this.registeredSample;
  }
}
