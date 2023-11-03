package life.qbic.projectmanagement.domain.model.experiment.exception;

/**
 * Thrown when a variable level exists, and the level's existence is unexpected.
 */
public class VariableLevelExistsException extends RuntimeException {

  public VariableLevelExistsException(String message) {
    super(message);
  }
}
