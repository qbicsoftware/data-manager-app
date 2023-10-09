package life.qbic.projectmanagement.domain.project.experiment.exception;

/**
 * Thrown when an experimental variable is not defined but is expected in an experiment's design.
 */
public class ExperimentalVariableNotDefinedException extends RuntimeException {

  public ExperimentalVariableNotDefinedException(String message) {
    super(message);
  }
}
