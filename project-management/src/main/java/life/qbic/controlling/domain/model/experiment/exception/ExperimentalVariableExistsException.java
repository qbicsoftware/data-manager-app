package life.qbic.controlling.domain.model.experiment.exception;

import java.io.Serial;
import life.qbic.controlling.domain.model.experiment.Experiment;

/**
 * <b>Experimental Variable Exists Exception</b>
 *
 * <p>
 * Within the experimental design setup, this exception can be used to indicate that a certain
 * experimental variable already exists within the experimental design.
 * <p>
 * This exception shall be only used in the context of
 * {@link Experiment}.
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
