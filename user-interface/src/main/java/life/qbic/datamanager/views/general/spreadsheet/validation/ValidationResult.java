package life.qbic.datamanager.views.general.spreadsheet.validation;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
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
