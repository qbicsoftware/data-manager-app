package life.qbic.projectmanagement.application.ontology;

/**
 * <b>Lookup Exception</b>
 * <p>
 * Throw to indicate that an exception occurred during the execution of the
 * {@link TerminologySelect} methods.
 *
 * @since 1.4.0
 */
public class LookupException extends RuntimeException {

  public LookupException() {
  }

  public LookupException(String message) {
    super(message);
  }

  public LookupException(String message, Throwable cause) {
    super(message, cause);
  }

  public LookupException(Throwable cause) {
    super(cause);
  }

  public LookupException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
