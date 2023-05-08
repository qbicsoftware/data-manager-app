package life.qbic.projectmanagement.domain.project.sample.event;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.project.sample.BatchId;

/**
 * <b>Batch Registered Event</b>
 * <p>
 * An event that indicates that a new batch has been registered
 *
 * @since 1.0.0
 */
public class BatchRegistered extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 580378782496926484L;

  private final BatchId batchId;

  private final Instant occurredOn;

  private BatchRegistered(Instant occurredOn, BatchId batchId) {
    this.occurredOn = Objects.requireNonNull(occurredOn);
    this.batchId = Objects.requireNonNull(batchId);
  }

  public static BatchRegistered create(BatchId batchId) {
    return new BatchRegistered(Instant.now(), batchId);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public BatchId batchId() {
    return this.batchId;
  }
}
