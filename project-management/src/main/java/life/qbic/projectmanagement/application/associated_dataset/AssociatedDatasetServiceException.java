package life.qbic.projectmanagement.application.associated_dataset;

import java.io.Serial;

/**
 * Abstract base of all checked exceptions thrown by
 * {@link AssociatedDatasetService} for unexpected (infrastructure-level) failure.
 *
 * <p>This class forms the exception contract of the service. Every
 * infrastructure exception (network failures, HTTP error codes,
 * JSON parse errors, timeouts) is translated into one of the two
 * concrete subclasses before propagating to callers. As a result,
 * callers never see infrastructure-level details (URLs, status codes,
 * raw error bodies) in exception messages — only user-friendly text
 * safe to display in the UI.</p>
 *
 * <p>The user-friendly message returned by {@link #userMessage()} is
 * in English and serves as the fallback text when no i18n bundle is
 * available. UI components that want to localise the message should
 * use the concrete exception type to choose an appropriate i18n key;
 * see {@link DatasetSourceUnavailableException} and
 * {@link DatasetSourceNotFoundException} for details.</p>
 *
 * <p>This class is {@code sealed} to a fixed set of subtypes so the
 * contract cannot silently grow — adding a new failure scenario
 * requires an explicit {@code permits} clause update and a documented
 * rationale.</p>
 *
 * @since 1.12.0
 */
public abstract sealed class AssociatedDatasetServiceException extends RuntimeException
    permits DatasetSourceUnavailableException, DatasetSourceNotFoundException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String userMessage;

  protected AssociatedDatasetServiceException(String userMessage) {
    super(userMessage);
    this.userMessage = userMessage;
  }

  protected AssociatedDatasetServiceException(String userMessage, Throwable cause) {
    super(userMessage, cause);
    this.userMessage = userMessage;
  }

  /**
   * @return the user-facing error message in English. This message
   *         contains no infrastructure details (no URLs, status codes,
   *         host names, or raw error bodies) and is safe to display
   *         directly to an end user.
   */
  public String userMessage() {
    return userMessage;
  }
}
