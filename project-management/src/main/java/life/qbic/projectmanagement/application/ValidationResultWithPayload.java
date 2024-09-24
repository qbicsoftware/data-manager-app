package life.qbic.projectmanagement.application;

import java.util.Objects;
import java.util.Optional;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record ValidationResultWithPayload<T>(ValidationResult validationResult, T payload) {

  public ValidationResultWithPayload(ValidationResult validationResult, T payload) {
    this.validationResult = Objects.requireNonNull(validationResult);
    this.payload = Objects.requireNonNull(payload);
  }

  public static ValidationResultWithPayload<ValidationResult> getValidationResultWithPayload(ValidationResult validationResult) {}
}
