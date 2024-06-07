package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Batch Updated Event</b>
 * <p>
 * An event that indicates that a batch has been updated
 *
 * @since 1.0.0
 */
public class BatchUpdated extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 580379846796926484L;

  private final BatchId batchId;
  private final ProjectId projectId;

  private BatchUpdated(BatchId batchId, ProjectId projectId) {
    this.batchId = Objects.requireNonNull(batchId);
    this.projectId = Objects.requireNonNull(projectId);
  }

  public static BatchUpdated create(BatchId id, ProjectId projectId) {
    return new BatchUpdated(id, projectId);
  }

  @JsonGetter("batchId")
  public BatchId batchId() {
    return this.batchId;
  }

  @JsonGetter("projectId")
  public ProjectId projectId() { return projectId; }

}
