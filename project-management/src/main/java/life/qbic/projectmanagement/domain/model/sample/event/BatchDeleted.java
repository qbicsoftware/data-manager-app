package life.qbic.projectmanagement.domain.model.sample.event;


import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.batch.BatchId;

/**
 * <b>Batch Deletion Event</b>
 * <p>
 * An event that indicates that a new batch has been registered
 *
 * @since 1.0.0
 */
public class BatchDeleted extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 365535713054819361L;
  private final BatchId batchId;
  private final Instant occurredOn;

  private BatchDeleted(Instant occurredOn, BatchId batchId) {
    this.occurredOn = Objects.requireNonNull(occurredOn);
    this.batchId = Objects.requireNonNull(batchId);
  }

  public static BatchDeleted create(BatchId batchId) {
    return new BatchDeleted(Instant.now(), batchId);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public BatchId batchId() {
    return this.batchId;
  }
}
