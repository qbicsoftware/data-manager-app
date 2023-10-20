package life.qbic.datamanager.views.general.spreadsheet;

import java.util.function.Predicate;

/**
 * Validates the content of a cell. Can be applied to a column.
 * <p>
 * One column can have multiple validators. The spreadsheet can use the validators to evaluate the
 * validity of cells in the column.
 */
public class ColumnValidator<T2> {

  private final Predicate<T2> predicate;
  private final String errorMessage;

  ColumnValidator(Predicate<T2> predicate, String errorMessage) {
    this.predicate = predicate;
    this.errorMessage = errorMessage;
  }

  public ValidationResult validate(T2 value) {
    boolean isValid = predicate.test(value);
    String filledErrorMessage = errorMessage.replaceAll("\\{0\\}", String.valueOf(value));
    return isValid ? ValidationResult.valid() : ValidationResult.invalid(filledErrorMessage);
  }

  public record ValidationResult(boolean isValid, String errorMessage) {

    public ValidationResult {
      if (isValid) {
        errorMessage = "";
      }
    }

    public static ValidationResult valid() {
      return new ValidationResult(true, "");
    }

    public static ValidationResult invalid(String errorMessage) {
      return new ValidationResult(false, errorMessage);
    }

    public boolean isInvalid() {
      return !isValid();
    }
  }
}
