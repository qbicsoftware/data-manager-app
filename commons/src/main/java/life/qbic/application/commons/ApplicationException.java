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

  public enum ErrorCode {
    GENERAL,
    INVALID_EXPERIMENTAL_DESIGN,
    INVALID_PROJECT_OBJECTIVE,
    INVALID_PROJECT_TITLE,
    INVALID_PROJECT_CODE,
    DUPLICATE_PROJECT_CODE,
    UNDEFINED_VARIABLE_LEVEL,
    NO_SPECIES_DEFINED,
    NO_SPECIMEN_DEFINED,
    NO_ANALYTE_DEFINED,
    ;

    @Override
    public String toString() {
      return this.getClass().getSimpleName() + "." + this.name();
    }

  }

  public record ErrorParameters(Object[] value) {

    public static ErrorParameters create() {
      return new ErrorParameters(new Object[]{});
    }

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

  private final ErrorCode errorCode;

  private final ErrorParameters errorParameters;

  public ApplicationException() {
    this(ErrorCode.GENERAL, ErrorParameters.create());
  }

  public ApplicationException(String message) {
    this(message, ErrorCode.GENERAL, ErrorParameters.create());
  }

  public ApplicationException(String message, Throwable cause) {
    this(message, cause, ErrorCode.GENERAL, ErrorParameters.create());
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

  public ErrorCode errorCode() {
    return errorCode;
  }

  public ErrorParameters errorParameters() {
    return errorParameters;
  }

  public ApplicationException wrapping(String message, Exception e) {
    if (e instanceof ApplicationException applicationException) {
      return new ApplicationException(message, e, applicationException.errorCode(),
          applicationException.errorParameters());
    } else {
      return new ApplicationException(message, e);
    }
  }

  public <E extends ApplicationException> ApplicationException wrapping(String message, E e) {
    return new ApplicationException(message, e, e.errorCode(), e.errorParameters());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ApplicationException.class.getSimpleName() + "[", "]")
        .add("message=" + getMessage())
        .add("errorCode=" + errorCode)
        .add("errorParameters=" + errorParameters)
        .toString();
  }
}
