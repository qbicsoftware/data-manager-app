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
  CONNECT_FAILED,

  /**
   * A dataset with the same persistent identifier (PID/DOI) is already
   * actively connected to the project. Prevents duplicate connections
   * to the same logical record.
   */
  ALREADY_CONNECTED,

  /**
   * A valid credential for the source instance is required to connect
   * this access-restricted dataset. The credential is needed to create
   * sharable access links for project collaborators.
   *
   * <p>Returned when the connecting user has no credential configured
   * for the instance, or the stored credential has been invalidated
   * (e.g. token rejected by the provider).</p>
   *
   * @since 1.12.0
   */
  CREDENTIAL_REQUIRED;

}
