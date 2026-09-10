package life.qbic.projectmanagement.application.associated_dataset;

import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import org.jspecify.annotations.Nullable;

/**
 * Per-dataset outcome of a sync trigger (DATSET-04/08).
 *
 * <p>Exactly one {@link SyncStatus} is set; {@link #error()} is non-null
 * only for {@link SyncStatus#FAILED}. For {@link SyncStatus#UPDATED} the
 * post-sync {@link #title()} / {@link #pid()} and the new version string
 * are carried so the UI and the summary notification can render the
 * record without re-querying.</p>
 *
 * @param datasetId          the connection (aggregate) id
 * @param status             the outcome category
 * @param previousVersion    the version before the sync (UPDATED only)
 * @param newVersion         the version after the sync, or null
 * @param accessStatusChanged whether the coarse access level changed
 * @param title              the record title after the sync (UPDATED only)
 * @param pid                the persistent identifier after the sync (UPDATED only)
 * @param error              the error code (FAILED only)
 * @since 1.13.0
 */
public record SyncDatasetResponse(
    AssociatedDatasetId datasetId,
    SyncStatus status,
    @Nullable String previousVersion,
    @Nullable String newVersion,
    boolean accessStatusChanged,
    @Nullable String title,
    @Nullable String pid,
    @Nullable SyncDatasetError error) {

  public enum SyncStatus {
    /** The local snapshot was replaced with newer metadata from the source. */
    UPDATED,
    /** The source already matches the local snapshot (no-op). */
    UP_TO_DATE,
    /** The sync could not be completed; {@link SyncDatasetResponse#error()} is set. */
    FAILED
  }

  public static SyncDatasetResponse updated(
      AssociatedDatasetId datasetId, String previousVersion, String newVersion,
      boolean accessStatusChanged, String title, String pid) {
    return new SyncDatasetResponse(datasetId, SyncStatus.UPDATED, previousVersion,
        newVersion, accessStatusChanged, title, pid, null);
  }

  public static SyncDatasetResponse upToDate(AssociatedDatasetId datasetId) {
    return new SyncDatasetResponse(datasetId, SyncStatus.UP_TO_DATE, null,
        null, false, null, null, null);
  }

  public static SyncDatasetResponse failed(
      AssociatedDatasetId datasetId, SyncDatasetError error) {
    return new SyncDatasetResponse(datasetId, SyncStatus.FAILED, null,
        null, false, null, null, error);
  }
}