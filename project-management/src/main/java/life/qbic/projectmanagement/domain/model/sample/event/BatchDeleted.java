package life.qbic.projectmanagement.domain.model.sample.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
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

  private BatchDeleted(BatchId batchId) {
    this.batchId = Objects.requireNonNull(batchId);
  }

  public static BatchDeleted create(BatchId batchId) {
    return new BatchDeleted(batchId);
  }

  @JsonGetter("batchId")
  public BatchId batchId() {
    return this.batchId;
  }
}
