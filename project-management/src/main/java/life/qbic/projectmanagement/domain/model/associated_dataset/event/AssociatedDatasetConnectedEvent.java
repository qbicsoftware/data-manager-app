package life.qbic.projectmanagement.domain.model.associated_dataset.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Domain event emitted when a dataset has been successfully connected to a project.
 *
 * <p>Drives project-member email notifications per ADR-0003 (N1).</p>
 *
 * @since 1.12.0
 */
public class AssociatedDatasetConnectedEvent extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 4726185049173920158L;

  private final AssociatedDatasetId associatedDatasetId;
  private final ProjectId projectId;
  private final String actorUserId;
  private final String datasetTitle;
  private final String datasetPid;

  private AssociatedDatasetConnectedEvent(
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

  public static AssociatedDatasetConnectedEvent create(
      AssociatedDatasetId associatedDatasetId,
      ProjectId projectId,
      String actorUserId,
      String datasetTitle,
      String datasetPid) {
    return new AssociatedDatasetConnectedEvent(
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
