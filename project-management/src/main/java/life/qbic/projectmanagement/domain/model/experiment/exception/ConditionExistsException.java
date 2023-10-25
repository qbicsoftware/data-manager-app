package life.qbic.projectmanagement.domain.model.experiment.exception;

/**
 * Thrown when condition exists with the same label, or the same levels.
 *
 * @since 1.0.0
 */
public class ConditionExistsException extends RuntimeException {


  public ConditionExistsException(String message) {
    super(message);
  }
}
