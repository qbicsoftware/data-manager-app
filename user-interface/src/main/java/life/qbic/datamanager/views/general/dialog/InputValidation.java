package life.qbic.datamanager.views.general.dialog;

import java.util.Objects;

/**
 * <b><record short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class InputValidation {

  private final ValidationStatus status;

  private InputValidation(ValidationStatus status) {
    this.status = Objects.requireNonNull(status);
  }

  public static InputValidation passed() {
    return new InputValidation(ValidationStatus.PASSED);
  }

  public static InputValidation failed() {
    return new InputValidation(ValidationStatus.FAILED);
  }

  enum ValidationStatus {
    PASSED, FAILED
  }

  public boolean hasPassed() {
    return status == ValidationStatus.PASSED;
  }

  public boolean hasFailed() {
    return status == ValidationStatus.FAILED;
  }

}
