package life.qbic.application.commons;

/**
 * Thrown whenever an exception occurred during an execution in the application layer. This
 * exception provides an error code and error parameters that are translated into a user-readable
 * message. Custom contexts provide an implementation of this class.
 * <p>
 * Each error code should help the user to change his input so that the functionality can be
 * completed correctly. Errors and exceptions independent of the user input should always be
 * assigned the GENERAL error code.
 *
 * @since 1.0.0
 */
public abstract class ApplicationException extends RuntimeException {

  public enum ErrorCode {
    GENERAL,
    INVALID_EXPERIMENTAL_DESIGN,
    INVALID_PROJECT_TITLE
  }

  public record ErrorParameters(Object[] value) {

    public static ErrorParameters create() {
      return new ErrorParameters(new Object[]{});
    }

    public static ErrorParameters of(Object... parameters) {
      return new ErrorParameters(parameters);
    }
  }

  private final ErrorCode errorCode;

  private final ErrorParameters errorParameters;

  protected ApplicationException() {
    this(ErrorCode.GENERAL, ErrorParameters.create());
  }

  protected ApplicationException(String message) {
    this(message, ErrorCode.GENERAL, ErrorParameters.create());
  }

  protected ApplicationException(String message, Throwable cause) {
    this(message, cause, ErrorCode.GENERAL, ErrorParameters.create());
  }

  protected ApplicationException(ErrorCode errorCode, ErrorParameters errorParameters) {
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  protected ApplicationException(String message, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(message);
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  protected ApplicationException(String message, Throwable cause, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(message, cause);
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  protected ApplicationException(Throwable cause, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(cause);
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  protected ApplicationException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, ErrorCode errorCode, ErrorParameters errorParameters) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }

  public ErrorParameters errorParameters() {
    return errorParameters;
  }
}
