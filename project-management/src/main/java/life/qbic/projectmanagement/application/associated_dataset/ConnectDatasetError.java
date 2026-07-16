package life.qbic.projectmanagement.application.associated_dataset;

/**
 * Error codes for the {@link AssociatedDatasetService#connectDataset}
 * operation.
 *
 * @since 1.12.0
 */
public enum ConnectDatasetError {

  /**
   * The specified source instance ID does not match any configured instance.
   */
  INSTANCE_NOT_FOUND,

  /**
   * The record identified by the external handle could not be found on the
   * source system (returned empty from {@link DatasetSource#resolveMetadata}).
   */
  RECORD_NOT_FOUND,

  /**
   * The dataset could not be connected due to an unexpected error.
   */
  CONNECT_FAILED;

}
