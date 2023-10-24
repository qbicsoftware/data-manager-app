package life.qbic.infrastructure.email;

import java.io.Serial;

/**
 * <b>Email Submission Exception</b>
 *
 * @since 1.0.0
 */
public class EmailSubmissionException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -8309431489482091477L;

  EmailSubmissionException(String message) {
    super(message);
  }

  EmailSubmissionException(String message, Throwable cause) {
    super(message, cause);
  }

}
