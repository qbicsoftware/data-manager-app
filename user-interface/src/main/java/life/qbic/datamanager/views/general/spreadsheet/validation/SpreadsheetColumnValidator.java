package life.qbic.datamanager.views.general.spreadsheet.validation;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class SpreadsheetColumnValidator {

  private final BiPredicate<List<String>, String> predicate;
  private final String errorMessage;

  public SpreadsheetColumnValidator(BiPredicate<List<String>, String> predicate,
      String errorMessage) {
    this.predicate = predicate;
    this.errorMessage = errorMessage;
  }

  public ValidationResult validate(List<String> values, String value) {

    boolean isValid = predicate.test(values, value);
    String filledErrorMessage = errorMessage.replaceAll("\\{0\\}", String.valueOf(value));
    return isValid ? ValidationResult.valid() : ValidationResult.invalid(filledErrorMessage);
  }

}
