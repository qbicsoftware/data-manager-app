package life.qbic.projectmanagement.domain.project.sample.event;

import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.Sample;

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
  private final Project project;
  private final String batchName;
  private final Instant occurredOn;

  private BatchRegistered(BatchId batchId, Instant occurredOn, Project project, String batchName) {
    this.batchId = Objects.requireNonNull(batchId);
    this.occurredOn = Objects.requireNonNull(occurredOn);
    this.project = Objects.requireNonNull(project);
    this.batchName = Objects.requireNonNull(batchName);
  }

  public static BatchRegistered create(String batchName, BatchId id, Project project) {
    return new BatchRegistered(id, Instant.now(), project, batchName);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public BatchId batchId() {
    return this.batchId;
  }

  public String name() { return batchName; }

  public Project project() { return project; }

}
