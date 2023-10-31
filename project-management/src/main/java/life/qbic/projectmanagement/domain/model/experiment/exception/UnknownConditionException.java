package life.qbic.projectmanagement.domain.model.experiment.exception;

import java.io.Serial;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;

/**
 * <b>Unknown Condition Exception</b>
 * <p>
 * During the experimental design setup, every sample group has one condition that is supposed to be
 * unambiguous within the design.
 * <p>
 * This exception shall only be used in the context of
 * {@link Experiment}.
 *
 * @since 1.0.0
 */
public class UnknownConditionException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -4767929765634452078L;

  public UnknownConditionException(String message) {
    super(message);
  }

}
