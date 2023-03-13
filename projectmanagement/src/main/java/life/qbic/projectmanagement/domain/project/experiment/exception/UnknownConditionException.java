package life.qbic.projectmanagement.domain.project.experiment.exception;

import java.io.Serial;

/**
 * <b>Unknown Condition Exception</b>
 * <p>
 * During the experimental design setup, every sample group has one condition that is supposed to be
 * unambiguous within the design.
 * <p>
 * This exception shall only be used in the context of
 * {@link life.qbic.projectmanagement.domain.project.experiment.Experiment}.
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
