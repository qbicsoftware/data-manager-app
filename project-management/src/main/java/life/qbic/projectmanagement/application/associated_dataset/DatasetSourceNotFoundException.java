package life.qbic.projectmanagement.application.associated_dataset;

import java.io.Serial;

/**
 * Thrown when a requested data repository identifier does not match
 * any configured instance.
 *
 * <p>In the {@link AssociatedDatasetService} this is raised by the
 * private {@code resolveInstanceConfig(instanceId)} helper when
 * {@link SourceInstanceRegistry#find(String)} returns empty. It
 * propagates as an exception from {@code searchDatasets(...)} and is
 * caught by {@code connectDataset(...)}, which translates it into
 * {@link ConnectDatasetError#INSTANCE_NOT_FOUND} (an expected-failure
 * result, not an exception).</p>
 *
 * <p>The user-facing message does not expose the unrecognised
 * {@code instanceId} — that identifier is a logical configuration
 * name (e.g. "zenodo", "fdat"), not an infrastructure URL, but the
 * end user cannot act on it either; the actionable advice is to
 * contact an administrator to verify repository settings.</p>
 *
 * <p><b>UI mapping guidance</b></p>
 * <ul>
 *   <li>Toast / inline toast: i18n key {@code dataset.source.not-found}</li>
 *   <li>Dialog title suggestion: <em>Repository Not Configured</em></li>
 * </ul>
 *
 * @since 1.12.0
 */
public final class DatasetSourceNotFoundException
    extends AssociatedDatasetServiceException {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String USER_MESSAGE =
      "The requested data repository is not configured. "
          + "Please verify the repository settings or contact your administrator.";

  /**
   * Constructs an exception with a user-friendly message. The raw
   * {@code instanceId} is recorded in the service log elsewhere, so
   * the message here is intentionally generic.
   */
  public DatasetSourceNotFoundException() {
    super(USER_MESSAGE);
  }

  /**
   * Constructs an exception with a user-friendly message. The raw
   * {@code instanceId} is recorded in the service log elsewhere, so
   * the message here is intentionally generic.
   *
   * @param cause optional underlying cause (rarely used — typically
   *     this exception is raised directly when the registry lookup
   *     fails)
   */
  public DatasetSourceNotFoundException(Throwable cause) {
    super(USER_MESSAGE, cause);
  }
}
