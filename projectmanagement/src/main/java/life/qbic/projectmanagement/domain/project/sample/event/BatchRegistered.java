package life.qbic.projectmanagement.domain.project.sample.event;

import java.io.Serial;
import java.time.Instant;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.project.sample.BatchId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class BatchRegistered extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 580378782496926484L;

  private final BatchId batchId;

  private final Instant occurredOn;

  private BatchRegistered(Instant occurredOn, BatchId batchId) {
    this.occurredOn = occurredOn;
    this.batchId = batchId;
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
