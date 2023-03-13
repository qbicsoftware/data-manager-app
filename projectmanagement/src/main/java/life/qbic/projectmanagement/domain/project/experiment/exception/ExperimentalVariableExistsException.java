package life.qbic.projectmanagement.domain.project.experiment.exception;

import java.io.Serial;

/**
 * <b>Experimental Variable Exists Exception</b>
 *
 * <p>
 * Within the experimental design setup, this exception can be used to indicate that a certain
 * experimental variable already exists within the experimental design.
 * <p>
 * This exception shall be only used in the context of
 * {@link life.qbic.projectmanagement.domain.project.experiment.Experiment}.
 *
 * @since 1.0.0
 */
public class ExperimentalVariableExistsException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 6749682039972762933L;

  public ExperimentalVariableExistsException(String message) {
    super(message);
  }

}
