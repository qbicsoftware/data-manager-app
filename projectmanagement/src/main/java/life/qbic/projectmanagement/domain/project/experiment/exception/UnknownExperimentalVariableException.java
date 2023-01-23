package life.qbic.projectmanagement.domain.project.experiment.exception;

import java.io.Serial;

/**
 * <b>Unknown Experimental Variable Exception</b>
 * <p>
 * In the context of experimental design creation an experimental variable is unambiguous. If access
 * to an existing variable is requested but it is not part of the design, then this exception can be
 * used.
 * <p>
 * This class shall be only used in the context of
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign}.
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
