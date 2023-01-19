package life.qbic.projectmanagement.domain.project.experiment.exception;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UnknownExperimentalVariableException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 4515321439513022125L;

  public UnknownExperimentalVariableException(String message) {
    super(message);
  }

}
