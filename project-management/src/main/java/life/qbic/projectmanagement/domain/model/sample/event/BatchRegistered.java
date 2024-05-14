package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Batch Registered Event</b>
 * <p>
 * An event that indicates that a new batch has been registered
 *
 * @since 1.0.0
 */
public class BatchRegistered extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 580378782496926484L;

  private final BatchId batchId;
  private final String projectTitle;
  private final String batchName;

  private BatchRegistered(BatchId batchId, String batchName, String projectTitle,
      ProjectId projectId) {
    super(projectId);
    this.batchId = Objects.requireNonNull(batchId);
    this.projectTitle = Objects.requireNonNull(projectTitle);
    this.batchName = Objects.requireNonNull(batchName);
  }

  public static BatchRegistered create(String batchName, BatchId id, String projectTitle,
      ProjectId projectId) {
    return new BatchRegistered(id, batchName, projectTitle, projectId);
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

  @JsonGetter("name")
  public String name() { return batchName; }

  @JsonGetter("projectTitle")
  public String projectTitle() { return projectTitle; }

}
