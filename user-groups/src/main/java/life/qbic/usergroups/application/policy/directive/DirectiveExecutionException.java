package life.qbic.usergroups.application.policy.directive;

/**
 * <b>Directive execution exception</b>
 *
 * <p>Thrown when a user-groups notification directive cannot be fulfilled (e.g. the recipient
 * user or the group can no longer be resolved). Mirrors {@code DirectiveExecutionException} of
 * the project management context.</p>
 *
 * @since 1.20.0
 */
public class DirectiveExecutionException extends RuntimeException {

  private static final long serialVersionUID = -6012222222222222223L;

  public DirectiveExecutionException(String message) {
    super(message);
  }

  public DirectiveExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}