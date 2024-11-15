package life.qbic.projectmanagement.application.policy.directive;

/**
 * <b>Policy Execution Exception </b>
 *
 * <p>Should be used to indicate an exception during the execution of a policy directive.</p>
 *
 * @since 1.7.0
 */
public class DirectiveExecutionException extends RuntimeException {
  public DirectiveExecutionException(String message) {
    super(message);
  }

}
