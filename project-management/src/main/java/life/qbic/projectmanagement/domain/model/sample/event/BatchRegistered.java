package life.qbic.projectmanagement.domain.model.sample.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Batch Registered Event</b>
 * <p>
 * An event that indicates that a new batch has been registered
 *
 * @since 1.0.0
 */
public class BatchRegistered extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 1439070961084871049L;

  private final BatchId batchId;
  private final String projectTitle;
  private final String batchName;
  private final ProjectId projectId;
  private final ExperimentId experimentId;

  private BatchRegistered(BatchId batchId, String batchName, String projectTitle,
      ProjectId projectId,
      ExperimentId experimentId) {
    this.batchId = Objects.requireNonNull(batchId);
    this.projectTitle = Objects.requireNonNull(projectTitle);
    this.batchName = Objects.requireNonNull(batchName);
    this.projectId = Objects.requireNonNull(projectId);
    this.experimentId = Objects.requireNonNull(experimentId);
  }

  public static BatchRegistered create(String batchName, BatchId id, String projectTitle,
      ProjectId projectId, ExperimentId experimentId) {
    return new BatchRegistered(id, batchName, projectTitle, projectId, experimentId);
  }

  @JsonGetter("batchId")
  public BatchId batchId() {
    return this.batchId;
  }

  @JsonGetter("name")
  public String name() { return batchName; }

  @JsonGetter("projectTitle")
  public String projectTitle() { return projectTitle; }

  @JsonGetter("projectId")
  public ProjectId projectId() { return projectId; }

  @JsonGetter("experimentId")
  public ExperimentId experimentId() {
    return experimentId;
  }
}
