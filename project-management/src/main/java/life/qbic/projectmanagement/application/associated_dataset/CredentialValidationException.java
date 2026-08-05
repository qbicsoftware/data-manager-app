package life.qbic.projectmanagement.application.associated_dataset;

/**
 * Thrown when credential validation cannot determine a definitive
 * result because of a transient infrastructure failure (network
 * error, remote server error after retries).
 *
 * <p>This is <em>not</em> thrown when a token is simply invalid
 * (401/403) — that case returns {@code false} from
 * {@link ExternalCredentialValidator#validateToken}. This exception
 * is reserved for situations where "try again later" might succeed.</p>
 *
 * @since 1.12.0
 */
public class CredentialValidationException extends RuntimeException {

  public CredentialValidationException(String message) {
    super(message);
  }

  public CredentialValidationException(String message, Throwable cause) {
    super(message, cause);
  }

}
