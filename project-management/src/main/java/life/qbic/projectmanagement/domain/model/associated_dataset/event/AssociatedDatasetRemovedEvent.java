package life.qbic.projectmanagement.domain.model.associated_dataset.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Domain event emitted when a dataset connection has been removed from a project.
 *
 * <p>Signals that a previously connected dataset is no longer actively linked.
 * The aggregate remains in the database in {@code REMOVED} state for audit purposes.</p>
 *
 * @since 1.12.0
 */
public class AssociatedDatasetRemovedEvent extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 2301797041948857105L;

  private final AssociatedDatasetId associatedDatasetId;
  private final ProjectId projectId;
  private final String actorUserId;
  private final String datasetTitle;
  private final String datasetPid;

  private AssociatedDatasetRemovedEvent(
      AssociatedDatasetId associatedDatasetId,
      ProjectId projectId,
      String actorUserId,
      String datasetTitle,
      String datasetPid) {
    this.associatedDatasetId = Objects.requireNonNull(associatedDatasetId);
    this.projectId = Objects.requireNonNull(projectId);
    this.actorUserId = Objects.requireNonNull(actorUserId);
    this.datasetTitle = Objects.requireNonNull(datasetTitle);
    this.datasetPid = Objects.requireNonNull(datasetPid);
  }

  /**
   * Factory method for creating a removal event.
   *
   * @param associatedDatasetId the dataset connection being removed
   * @param projectId           the project the dataset was connected to
   * @param actorUserId         the user who performed the removal
   * @param datasetTitle        human-readable title of the dataset
   * @param datasetPid          persistent identifier (DOI/PID) of the dataset
   * @return the constructed event
   */
  public static AssociatedDatasetRemovedEvent create(
      AssociatedDatasetId associatedDatasetId,
      ProjectId projectId,
      String actorUserId,
      String datasetTitle,
      String datasetPid) {
    return new AssociatedDatasetRemovedEvent(
        associatedDatasetId, projectId, actorUserId, datasetTitle, datasetPid);
  }

  @JsonGetter("associatedDatasetId")
  public AssociatedDatasetId associatedDatasetId() {
    return associatedDatasetId;
  }

  @JsonGetter("projectId")
  public ProjectId projectId() {
    return projectId;
  }

  @JsonGetter("actorUserId")
  public String actorUserId() {
    return actorUserId;
  }

  @JsonGetter("datasetTitle")
  public String datasetTitle() {
    return datasetTitle;
  }

  @JsonGetter("datasetPid")
  public String datasetPid() {
    return datasetPid;
  }
}