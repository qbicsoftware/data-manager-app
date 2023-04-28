package life.qbic.projectmanagement.domain.project.experiment.exception;

import java.io.Serial;

/**
 * <b>Unknown Variable Level Exception</b>
 * <p>
 * Indicates, that a certain
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalValue} is not part of a
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable}.
 *
 * @since 1.0.0
 */
public class UnknownVariableLevelException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -2616683268074946569L;

  public UnknownVariableLevelException(String message) {
    super(message);
  }
}
