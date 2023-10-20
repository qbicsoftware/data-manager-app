package life.qbic.infrastructure.email;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
