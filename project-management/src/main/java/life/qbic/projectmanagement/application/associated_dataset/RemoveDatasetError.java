package life.qbic.projectmanagement.application.associated_dataset;

/**
 * Error codes for the {@link AssociatedDatasetService#removeDataset} operation.
 *
 * @since 1.12.0
 */
public enum RemoveDatasetError {

  /**
   * The specified dataset connection was not found in the repository.
   * May indicate the ID is invalid or the dataset does not belong to the
   * calling user's scope.
   */
  DATASET_NOT_FOUND,

  /**
   * The dataset connection is already in {@code REMOVED} state.
   * Removal is idempotent — calling remove on an already-removed
   * connection returns this error rather than succeeding silently.
   */
  DATASET_ALREADY_REMOVED,

  /**
   * The dataset could not be removed due to an unexpected error
   * (e.g., persistence failure, constraint violation).
   */
  REMOVAL_FAILED;
}