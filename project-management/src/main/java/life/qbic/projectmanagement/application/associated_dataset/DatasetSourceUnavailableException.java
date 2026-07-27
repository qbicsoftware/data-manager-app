package life.qbic.projectmanagement.application.associated_dataset;

import java.io.Serial;

/**
 * Thrown when an external data repository is unreachable or fails to
 * respond correctly.
 *
 * <p>Covers infrastructure-level failures such as:</p>
 * <ul>
 *   <li>network / connection errors</li>
 *   <li>HTTP request timeouts</li>
 *   <li>rate-limit exhaustion (HTTP 429 after retries)</li>
 *   <li>server errors (HTTP 5xx after retries)</li>
 *   <li>response body cannot be parsed as expected (malformed / unexpected JSON)</li>
 *   <li>authentication / credential resolution errors on the source system</li>
 * </ul>
 *
 * <p>The user-facing message deliberately omits the affected instance
 * name, URL, and HTTP status code — those details are available in the
 * application log (the original cause is preserved via
 * {@link #getCause()}) but must not reach the end user.</p>
 *
 * <p><b>UI mapping guidance</b></p>
 * <ul>
 *   <li>Toast / inline toast: i18n key {@code dataset.search.failed}</li>
 *   <li>Dialog title suggestion: <em>Repository Unavailable</em></li>
 * </ul>
 *
 * @since 1.12.0
 */
public final class DatasetSourceUnavailableException
    extends AssociatedDatasetServiceException {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final String DEFAULT_MESSAGE =
      "The data repository is temporarily unavailable. Please try again in a moment.";

  /** Constructs an exception with the default user-friendly message. */
  public DatasetSourceUnavailableException() {
    super(DEFAULT_MESSAGE, null);
  }

  /** Constructs an exception with the default user-friendly message, preserving the underlying cause. */
  public DatasetSourceUnavailableException(Throwable cause) {
    super(DEFAULT_MESSAGE, cause);
  }

  /** Constructs an exception with a custom user-friendly message. */
  public DatasetSourceUnavailableException(String customUserMessage) {
    super(customUserMessage, null);
  }

  /** Constructs an exception with a custom user-friendly message, preserving the underlying cause. */
  public DatasetSourceUnavailableException(String customUserMessage, Throwable cause) {
    super(customUserMessage, cause);
  }
}
