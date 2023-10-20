package life.qbic.controlling.domain.model.experiment.exception;

import java.io.Serial;
import life.qbic.controlling.domain.model.experiment.ExperimentalValue;
import life.qbic.controlling.domain.model.experiment.ExperimentalVariable;

/**
 * <b>Unknown Variable Level Exception</b>
 * <p>
 * Indicates, that a certain
 * {@link ExperimentalValue} is not part of a
 * {@link ExperimentalVariable}.
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
