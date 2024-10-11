package life.qbic.projectmanagement.application;

import java.util.Objects;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record ValidationResultWithPayload<T>(ValidationResult validationResult, T payload) {

  public ValidationResultWithPayload {
    Objects.requireNonNull(validationResult);
  }
}
