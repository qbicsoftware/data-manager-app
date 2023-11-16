package life.qbic.datamanager.views.general.spreadsheet.validation;

import java.util.function.BiPredicate;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class SpreadsheetObjectValidator<S, T> {

  private final BiPredicate<S, T> predicate;
  private final String errorMessage;

  public SpreadsheetObjectValidator(BiPredicate<S, T> predicate, String errorMessage) {
    this.predicate = predicate;
    this.errorMessage = errorMessage;
  }

  public ValidationResult validate(S object, T value) {
    boolean isValid = predicate.test(object, value);
    String filledErrorMessage = errorMessage.replaceAll("\\{0\\}", String.valueOf(value));
    return isValid ? ValidationResult.valid() : ValidationResult.invalid(filledErrorMessage);
  }
}
