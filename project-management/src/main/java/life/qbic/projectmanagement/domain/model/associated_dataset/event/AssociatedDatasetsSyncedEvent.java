package life.qbic.projectmanagement.domain.model.associated_dataset.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Domain event emitted once per sync trigger (single dataset or Sync All)
 * after at least one connected dataset was updated with fresh metadata
 * from the source platform (DATSET-04/08, ADR-0005).
 *
 * <p>Carries the list of records that were actually updated so the
 * notification directive can send <em>one</em> combined email per project
 * member instead of flooding recipients with per-record emails. The event
 * is <strong>not</strong> emitted for no-op syncs ("already up to date")
 * or for failures — those surface to the invoking user in the sync
 * results sidecar.</p>
 *
 * @since 1.13.0
 */
public class AssociatedDatasetsSyncedEvent extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 7426135049173920159L;

  private final ProjectId projectId;
  private final String actorUserId;
  private final List<UpdatedRecord> updatedRecords;

  private AssociatedDatasetsSyncedEvent(
      ProjectId projectId,
      String actorUserId,
      List<UpdatedRecord> updatedRecords) {
    this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
    this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    this.updatedRecords = List.copyOf(
        Objects.requireNonNull(updatedRecords, "updatedRecords must not be null"));
  }

  /**
   * Creates the summary event for a sync trigger.
   *
   * @param projectId      the project the datasets belong to
   * @param actorUserId    the user who triggered the sync
   * @param updatedRecords the records updated during this trigger; must
   *                       not be empty (callers skip emission otherwise)
   * @return the event
   */
  public static AssociatedDatasetsSyncedEvent create(
      ProjectId projectId,
      String actorUserId,
      List<UpdatedRecord> updatedRecords) {
    return new AssociatedDatasetsSyncedEvent(projectId, actorUserId, updatedRecords);
  }

  @JsonGetter("projectId")
  public ProjectId projectId() {
    return projectId;
  }

  @JsonGetter("actorUserId")
  public String actorUserId() {
    return actorUserId;
  }

  @JsonGetter("updatedRecords")
  public List<UpdatedRecord> updatedRecords() {
    return updatedRecords;
  }

  /**
   * A single record that was updated during a sync trigger.
   *
   * @param datasetId          the connection (aggregate) id
   * @param title              the record title
   * @param pid                the persistent identifier (DOI) after the update
   * @param previousVersion    the version before the sync, or null
   * @param newVersion         the version after the sync, or null
   * @param accessStatusChanged whether the coarse access level changed
   *                            (e.g. embargo lifted or added)
   */
  public record UpdatedRecord(
      AssociatedDatasetId datasetId,
      String title,
      String pid,
      String previousVersion,
      String newVersion,
      boolean accessStatusChanged) {

    public UpdatedRecord {
      Objects.requireNonNull(datasetId, "datasetId must not be null");
      Objects.requireNonNull(title, "title must not be null");
      Objects.requireNonNull(pid, "pid must not be null");
    }
  }
}