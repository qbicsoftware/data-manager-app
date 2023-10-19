package life.qbic.projectmanagement.domain.project.sample.event;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.project.ProjectId;
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
  private final String projectTitle;
  private final ProjectId projectId;
  private final String batchName;
  private final Instant occurredOn;

  private BatchRegistered(BatchId batchId, Instant occurredOn, String batchName, String projectTitle,
      ProjectId projectId) {
    this.batchId = Objects.requireNonNull(batchId);
    this.occurredOn = Objects.requireNonNull(occurredOn);
    this.projectTitle = Objects.requireNonNull(projectTitle);
    this.projectId = Objects.requireNonNull(projectId);
    this.batchName = Objects.requireNonNull(batchName);
  }

  public static BatchRegistered create(String batchName, BatchId id, String projectTitle,
      ProjectId projectId) {
    return new BatchRegistered(id, Instant.now(), batchName, projectTitle, projectId);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public BatchId batchId() {
    return this.batchId;
  }

  public String name() { return batchName; }

  public String projectTitle() { return projectTitle; }

  public ProjectId projectId() { return projectId; }

}
