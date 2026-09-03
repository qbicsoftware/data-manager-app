package life.qbic.projectmanagement.application.associated_dataset;

/**
 * Thrown when a previously created access link cannot be revoked
 * (deleted) on the source system.
 *
 * <p>This occurs when the remote service is unavailable, returns a
 * server error, or denies the operation. A revoked link is a clean-up
 * action — it is never allowed to fail the primary operation that
 * triggered it (connect rollback or connection removal), so callers
 * treat {@code AccessLinkRevocationException} as a best-effort failure
 * that must be logged but not propagated as a hard error.</p>
 *
 * @since 1.12.0
 */
public class AccessLinkRevocationException extends RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public AccessLinkRevocationException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause   the cause of this exception
   */
  public AccessLinkRevocationException(String message, Throwable cause) {
    super(message, cause);
  }
}