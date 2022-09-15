package life.qbic.application.commons;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public abstract class ApplicationException extends RuntimeException {

  public enum ErrorCode {
    GENERAL,
    INVALID_PROJECT_TITLE
  }

  public static class ErrorParameters {

    private final HashMap<String, Object> mappings = new HashMap<>();

    public Optional<Object> get(String key) {
      return Optional.ofNullable(mappings.get(key));
    }

    public void put(String key, Object value) {
      mappings.put(key, value);
    }

    public static ErrorParameters create() {
      return new ErrorParameters();
    }

    public ErrorParameters with(String key, Object value) {
      this.put(key, value);
      return this;
    }

    public Map<String, Object> asMap() {
      return unmodifiableMap(mappings);
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
