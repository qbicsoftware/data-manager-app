package life.qbic.projectmanagement.application.associated_dataset;

/**
 * Thrown when an access link cannot be created for a restricted dataset.
 *
 * <p>This occurs when the user lacks permission to create access links
 * on the remote record (e.g., they are not the record owner), or when
 * the remote service is unavailable.</p>
 *
 * <p>The application service catches this exception and returns
 * {@link ConnectDatasetError#ACCESS_LINK_CREATION_FAILED} to the UI,
 * which displays a user-friendly error message.</p>
 *
 * @since 1.12.0
 */
public class AccessLinkCreationException extends RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public AccessLinkCreationException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause   the cause of this exception
   */
  public AccessLinkCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}
