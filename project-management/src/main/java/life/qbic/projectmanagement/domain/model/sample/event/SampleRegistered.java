package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Sample Registered - Domain Event</b>
 * <p>
 * A new physical sample has been registered and prepared for measurement in the lab.
 *
 * @since 1.0.0
 */
public class SampleRegistered extends DomainEvent {

  @Serial
  private static final long serialVersionUID = -1338442721083240618L;

  @JsonProperty("batchId")
  private final BatchId assignedBatch;

  @JsonProperty("sampleId")
  private final SampleId registeredSample;

  private SampleRegistered(BatchId assignedBatch, SampleId registeredSample) {
    this.assignedBatch = Objects.requireNonNull(assignedBatch);
    this.registeredSample = Objects.requireNonNull(registeredSample);
  }

  /**
   * Creates a new {@link SampleRegistered} object instance.
   *
   * @param assignedBatch    the batch reference the sample will be assigned to
   * @param registeredSample the sample reference of the newly registered physical sample
   * @return a new instance of this domain event
   * @since 1.0.0
   */
  public static SampleRegistered create(BatchId assignedBatch, SampleId registeredSample) {
    return new SampleRegistered(assignedBatch, registeredSample);
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
