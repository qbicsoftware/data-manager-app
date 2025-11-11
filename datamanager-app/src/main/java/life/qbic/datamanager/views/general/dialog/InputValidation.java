package life.qbic.datamanager.views.general.dialog;

import java.util.Objects;
import java.util.StringJoiner;
import org.springframework.lang.NonNull;

/**
 * <b>Input Validation</b>
 *
 * <p>Simple validation result indication for {@link UserInput} validations.</p>
 *
 * @since 1.7.0
 */
public class InputValidation {

  private final ValidationStatus status;

  private InputValidation(ValidationStatus status) {
    this.status = Objects.requireNonNull(status);
  }

  /**
   * Creates a {@link InputValidation} that represents a successful validation.
   *
   * @return the {link InputValidation}
   * @since 1.7.0
   */
  public static InputValidation passed() {
    return new InputValidation(ValidationStatus.PASSED);
  }

  /**
   * Creates a {@link InputValidation} that represents a failing validation.
   *
   * @return the {link InputValidation}
   * @since 1.7.0
   */
  public static InputValidation failed() {
    return new InputValidation(ValidationStatus.FAILED);
  }

  public boolean hasPassed() {
    return status == ValidationStatus.PASSED;
  }

  enum ValidationStatus {
    PASSED, FAILED
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", InputValidation.class.getSimpleName() + "[", "]")
        .add("status=" + status)
        .toString();
  }

  public InputValidation and(InputValidation other) {
    return other.hasPassed() && hasPassed() ? passed() : failed();
  }

  public InputValidation or(InputValidation other) {
    return other.hasPassed() || hasPassed() ? passed() : failed();
  }

  public void ifPassed(@NonNull DialogAction dialogAction) {
    if (hasPassed()) {
      dialogAction.execute();
    }
  }
}
