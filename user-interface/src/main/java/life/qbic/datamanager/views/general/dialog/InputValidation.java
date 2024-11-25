package life.qbic.datamanager.views.general.dialog;

import java.util.Objects;
import java.util.function.Consumer;
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

  public boolean hasFailed() {
    return status == ValidationStatus.FAILED;
  }

  enum ValidationStatus {
    PASSED, FAILED
  }

  public void onPassed(@NonNull Action action) {
    if (hasPassed()) {
      action.execute();
    }
  }
}
