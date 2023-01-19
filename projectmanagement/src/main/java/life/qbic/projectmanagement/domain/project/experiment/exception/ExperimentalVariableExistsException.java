package life.qbic.projectmanagement.domain.project.experiment.exception;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentalVariableExistsException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 6749682039972762933L;

  public ExperimentalVariableExistsException(String message) {
    super(message);
  }

}
