package life.qbic.projectmanagement.application.associated_dataset;

/**
 * Error codes for synchronising a connected dataset with its source
 * instance (DATSET-04/08, ADR-0005).
 *
 * @since 1.13.0
 */
public enum SyncDatasetError {

  /**
   * The dataset connection does not exist or is already removed.
   */
  DATASET_NOT_FOUND,

  /**
   * The record (and its version chain) no longer exists on the source
   * system. The connection is kept — the user decides whether to remove
   * it manually.
   */
  RECORD_NOT_FOUND,

  /**
   * The dataset is access-restricted and the invoking user has no valid
   * credential configured for the source instance. Guidance: configure
   * the provider (add a token) first.
   */
  CREDENTIAL_REQUIRED,

  /**
   * The dataset is access-restricted and the invoking user's token does
   * not grant access to the record on the source system.
   */
  CREDENTIAL_INSUFFICIENT,

  /**
   * A restricted dataset moved to a new version, but the shareable
   * access link could not be created on the latest record. Per
   * ADR-0005 (L1) the sync fails atomically — only the record owner can
   * refresh the access link.
   */
  ACCESS_LINK_REFRESH_FAILED,

  /**
   * The target version (PID) of the sync is already connected to the
   * project through another active connection. The connection is left
   * untouched to avoid duplicates.
   *
   * @since 1.13.0
   */
  ALREADY_CONNECTED,

  /**
   * The sync could not be completed due to an unexpected or transient
   * error (network, rate limit, persistence failure, …).
   */
  SYNC_FAILED;

}