package life.qbic.projectmanagement.application.associated_dataset;

import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;

import java.io.Serial;

/**
 * Thrown by {@link DatasetSource} when a metadata resolve operation fails
 * due to an infrastructure-level error.
 *
 * <p>Represents failures in {@link DatasetSource#resolveMetadata(String,
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
 * <p>A record that does not exist on the source system (HTTP 404) is
 * <em>not</em> represented by this exception. Instead,
 * {@link DatasetSource#resolveMetadata} returns {@link java.util.Optional#empty()}
 * for that case.</p>
 *
 * <p>The cause chain preserves the original exception (e.g. the
 * underlying I/O error or HTTP error from the client) so callers can
 * inspect technical details when needed.</p>
 *
 * @since 1.12.0
 */
public final class DatasetResolveException extends ApplicationException {

  @Serial
  private static final long serialVersionUID = 1L;

  public DatasetResolveException(String message) {
    super(message);
  }

  /**
   * Constructs a resolve exception with the given message and cause.
   *
   * @param message user-facing or debug message describing the failure
   * @param cause   the underlying cause (network error, HTTP error, etc.)
   */
  public DatasetResolveException(String message, Throwable cause) {
    super(message, cause);
  }
}
