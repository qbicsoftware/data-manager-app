package life.qbic.projectmanagement;

/**
 * Thrown whenever the domain ends up in an exceptional state.
 */
public class ProjectManagementDomainException extends RuntimeException {

  public ProjectManagementDomainException() {
  }

  public ProjectManagementDomainException(String message) {
    super(message);
  }

  public ProjectManagementDomainException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProjectManagementDomainException(Throwable cause) {
    super(cause);
  }

  public ProjectManagementDomainException(String message, Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
