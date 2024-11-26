package life.qbic.application.commons;

import java.util.Arrays;
import java.util.StringJoiner;

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
public class ApplicationException extends RuntimeException {

  private final ErrorCode errorCode;
  private final transient ErrorParameters errorParameters;

  public ApplicationException() {
    this(ErrorCode.GENERAL, ErrorParameters.empty());
  }

  public ApplicationException(String message) {
    this(message, ErrorCode.GENERAL, ErrorParameters.empty());
  }

  public ApplicationException(String message, Throwable cause) {
    this(message, cause, ErrorCode.GENERAL, ErrorParameters.empty());
  }

  public ApplicationException(ErrorCode errorCode, ErrorParameters errorParameters) {
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  public ApplicationException(String message, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(message);
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  public ApplicationException(String message, Throwable cause, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(message, cause);
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  public ApplicationException(Throwable cause, ErrorCode errorCode,
      ErrorParameters errorParameters) {
    super(cause);
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  public ApplicationException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, ErrorCode errorCode, ErrorParameters errorParameters) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.errorCode = errorCode;
    this.errorParameters = errorParameters;
  }

  /**
   * Wraps the provided throwable into an ApplicationException and sets the provided error message.
   * <p>
   * If the throwable is an ApplicationException itself, the wrapping exception will have the same
   * error code and error parameters as the wrapped exception.
   * <p>
   * If the throwable is not an ApplicationException, the general error code and error parameters
   * will be used.
   * <p>
   * The wrapped throwable is set as cause of the wrapping ApplicationException.
   *
   * @param message the debug message to set for the new application exception
   * @param e       the throwable to be wrapped
   * @return an ApplicationException with the message, an appropriate error code and e set as cause.
   */
  public static ApplicationException wrapping(String message, Throwable e) {
    if (e instanceof ApplicationException applicationException) {
      return new ApplicationException(message, e, applicationException.errorCode(),
          applicationException.errorParameters());
    } else {
      return new ApplicationException(message, e);
    }
  }

  /**
   * Wraps the provided throwable into an ApplicationException.
   * <p>
   * If the throwable is an ApplicationException itself, the wrapping exception will have the same
   * error code and error parameters as the wrapped exception; Otherwise the general error code and
   * error parameters will be used.
   * <p>
   * The wrapped throwable is set as cause of the wrapping ApplicationException.
   *
   * @param e the throwable to be wrapped
   * @return an ApplicationException with an appropriate error code and e set as cause.
   */
  public static ApplicationException wrapping(Throwable e) {
    if (e instanceof ApplicationException applicationException) {
      return new ApplicationException(e, applicationException.errorCode(),
          applicationException.errorParameters());
    }
    if (e instanceof org.springframework.security.access.AccessDeniedException) {
      return new ApplicationException(e, ErrorCode.ACCESS_DENIED, ErrorParameters.empty());
    } else {
      return new ApplicationException(e.getMessage(), e);
    }
  }

  public ErrorCode errorCode() {
    return errorCode;
  }

  public ErrorParameters errorParameters() {
    return errorParameters;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ApplicationException.class.getSimpleName() + "[", "]")
        .add("message=" + getMessage())
        .add("errorCode=" + errorCode)
        .add("errorParameters=" + errorParameters)
        .toString();
  }

  /**
   * The error code of an ApplicationException. This code can be used to determine what kind of
   * error occurred. This allows for the message of the ApplicationException to hold debug
   * information and avoids string matching to search for the error message.
   */
  public enum ErrorCode {
    GENERAL,
    ACCESS_DENIED,
    INVALID_EXPERIMENTAL_DESIGN,
    INVALID_PROJECT_OBJECTIVE,
    INVALID_PROJECT_TITLE,
    INVALID_PROJECT_CODE,
    DUPLICATE_PROJECT_CODE,
    DUPLICATE_GROUP_SELECTED,
    UNDEFINED_VARIABLE_LEVEL,
    NO_CONDITION_SELECTED,
    NO_SPECIES_DEFINED,
    NO_SPECIMEN_DEFINED,
    NO_ANALYTE_DEFINED,
    DATA_ATTACHED_TO_SAMPLES,
    SAMPLES_ATTACHED_TO_EXPERIMENT,
    SERVICE_FAILED,
    UNKNOWN_METADATA;

    @Override
    public String toString() {
      return this.getClass().getSimpleName() + "." + this.name();
    }

  }

  /**
   * Error parameters to be used in error messages.
   *
   * @param value an ordered array of parameters
   */
  public record ErrorParameters(Object[] value) {

    /**
     * Creates a new instance of an empty error parameter array
     *
     * @return
     * @since 1.0.0
     */
    public static ErrorParameters empty() {
      return new ErrorParameters(new Object[]{});
    }

    /**
     * Creates a new instance with the provided error parameters
     *
     * @param parameters the error parameters
     * @return
     * @since 1.0.0
     */
    public static ErrorParameters of(Object... parameters) {
      return new ErrorParameters(parameters);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ErrorParameters that = (ErrorParameters) o;

      return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", ErrorParameters.class.getSimpleName() + "[", "]")
          .add("value=" + Arrays.toString(value))
          .toString();
    }
  }
}
