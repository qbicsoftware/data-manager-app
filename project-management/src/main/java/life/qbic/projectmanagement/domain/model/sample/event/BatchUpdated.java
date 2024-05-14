package life.qbic.projectmanagement.domain.model.sample.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Batch Update Event</b>
 * <p>
 * An event that indicates that a batch has been updated
 *
 * @since 1.0.0
 */
public class BatchUpdated extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 365535713054819361L;
  private final BatchId batchId;

  private BatchUpdated(BatchId batchId, ProjectId projectId) {
    super(projectId);
    this.batchId = Objects.requireNonNull(batchId);
  }

  public static BatchUpdated create(BatchId batchId, ProjectId projectId) {
    return new BatchUpdated(batchId, projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  @JsonGetter("batchId")
  public BatchId batchId() {
    return this.batchId;
  }
}
