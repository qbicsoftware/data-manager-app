package life.qbic.projectmanagement.application.associated_dataset;

import java.io.Serial;
import life.qbic.application.commons.ApplicationException;

/**
 * Thrown by {@link DatasetSource} when a dataset search operation fails
 * due to an infrastructure-level error.
 *
 * <p>Represents failures in {@link DatasetSource#search(SearchQuery,
 * InstanceConfig, String)} such as:</p>
 * <ul>
 *   <li>network / connection errors</li>
 *   <li>HTTP request timeouts</li>
 *   <li>rate-limit exhaustion (HTTP 429 after retries)</li>
 *   <li>server errors (HTTP 5xx after retries)</li>
 *   <li>response body cannot be parsed as expected</li>
 *   <li>authentication / credential resolution errors on the source system</li>
 * </ul>
 *
 * <p>The cause chain preserves the original exception (e.g. the
 * underlying I/O error or HTTP error from the client) so callers can
 * inspect technical details when needed.</p>
 *
 * @since 1.12.0
 */
public final class DatasetSearchException extends ApplicationException {

  @Serial
  private static final long serialVersionUID = 1L;

  public DatasetSearchException(String message) {
    super(message);
  }

  /**
   * Constructs a search exception with the given message and cause.
   *
   * @param message user-facing or debug message describing the failure
   * @param cause   the underlying cause (network error, HTTP error, etc.)
   */
  public DatasetSearchException(String message, Throwable cause) {
    super(message, cause);
  }
}
