package life.qbic.projectmanagement.application;

import life.qbic.application.commons.ApplicationException;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class ProjectManagementException extends ApplicationException {

  public ProjectManagementException() {
  }

  public ProjectManagementException(String message) {
    super(message);
  }

  public ProjectManagementException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProjectManagementException(ErrorCode errorCode, ErrorParameters errorParameters) {
    super(errorCode, errorParameters);
  }

  public ProjectManagementException(String message, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(message, errorCode, errorParameters);
  }

  public ProjectManagementException(String message, Throwable cause, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(message, cause, errorCode, errorParameters);
  }

  public ProjectManagementException(Throwable cause, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(cause, errorCode, errorParameters);
  }

  public ProjectManagementException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, ErrorCode errorCode, ErrorParameters errorParameters) {
    super(message, cause, enableSuppression, writableStackTrace, errorCode, errorParameters);
  }
}
