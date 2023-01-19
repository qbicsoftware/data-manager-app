package life.qbic.projectmanagement.domain.project.experiment.exception;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SampleGroupExistsException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 7632553386822112535L;

  public SampleGroupExistsException(String message) {
    super(message);
  }

}
