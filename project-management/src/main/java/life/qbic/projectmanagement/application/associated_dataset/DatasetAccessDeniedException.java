package life.qbic.projectmanagement.application.associated_dataset;

import java.io.Serial;

/**
 * Thrown by {@link DatasetSource#resolveLatest(String, InstanceConfig, String)}
 * when the source system denies access to the record (HTTP 401/403).
 *
 * <p>Distinguishes <em>access/credential</em> failures from transient
 * infrastructure failures during sync (ADR-0005): the application layer
 * maps this to {@link SyncDatasetError#CREDENTIAL_REQUIRED} (no usable
 * credential configured) or {@link SyncDatasetError#CREDENTIAL_INSUFFICIENT}
 * (a credential exists but does not grant access to the record). A record
 * that does not exist (404) is still represented by
 * {@link java.util.Optional#empty()}.</p>
 *
 * @since 1.13.0
 */
public final class DatasetAccessDeniedException extends DatasetResolveException {

  @Serial
  private static final long serialVersionUID = 1L;

  public DatasetAccessDeniedException(String message) {
    super(message);
  }

  public DatasetAccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }
}