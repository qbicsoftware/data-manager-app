package life.qbic.datamanager.views.general.spreadsheet.validation;

import java.util.function.Predicate;

/**
 * Validates the content of a cell.
 * <p>
 * The spreadsheet can use the validators to evaluate the validity of cells.
 *
 * @param <T> the type this validator can validate
 */
public class SpreadsheetCellValidator<T> {

  private final Predicate<T> predicate;
  private final String errorMessage;

  public SpreadsheetCellValidator(Predicate<T> predicate, String errorMessage) {
    this.predicate = predicate;
    this.errorMessage = errorMessage;
  }

  public ValidationResult validate(T value) {
    boolean isValid = predicate.test(value);
    String filledErrorMessage = errorMessage.replaceAll("\\{0\\}", String.valueOf(value));
    return isValid ? ValidationResult.valid() : ValidationResult.invalid(filledErrorMessage);
  }

}
