package life.qbic.projectmanagement.domain.model.experiment.exception;

import java.io.Serial;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;

/**
 * <b>Sample Group Exists Exception</b>
 *
 * <p>
 * Within the experimental design setup, this exception indicates that a sample group already exists
 * within the design.
 * <p>
 * This exception class shall only be used in the context of the
 * {@link Experiment} behaviour.
 *
 * @since 1.0.0
 */
public class SampleGroupExistsException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 7632553386822112535L;

  public SampleGroupExistsException(String message) {
    super(message);
  }

}
