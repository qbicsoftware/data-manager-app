package life.qbic.projectmanagement.application.validation;

import java.util.List;
import java.util.Map;
import life.qbic.projectmanagement.application.ValidationResult;

/**
 * <b>Free-text length validation</b>
 *
 * <p>Formats the failure for a value that exceeds a maximum length. The maximum is a parameter and
 * deliberately not defined here: each storage column owns its width, so sample comments, sample
 * batch and each measurement type can diverge without one edit silently changing all of them.
 *
 * <p>The limits themselves live with their concern, e.g. {@code SampleFieldLength} and
 * {@code MeasurementFieldLength}.
 *
 * <p>See #1695 and #1706: the columns were {@code varchar(255)} while the templates advertised 500
 * characters and nothing validated either number, so MariaDB rejected writes at flush time.
 *
 * @since 1.20.3
 */
public final class LengthValidation {

  private LengthValidation() {
    // static utility
  }

  /**
   * Validates the length of a free-text value without identifying the affected row.
   *
   * @param fieldName     the user-facing name of the field, e.g. {@code "Comment"}
   * @param value         the value to validate; {@code null} is treated as empty
   * @param maxCharacters the maximum length allowed by the storage column
   */
  public static ValidationResult validate(String fieldName, String value, int maxCharacters) {
    return validate(fieldName, value, maxCharacters, null);
  }

  /**
   * Validates the length of a free-text value.
   *
   * @param fieldName     the user-facing name of the field, e.g. {@code "Comment"}
   * @param value         the value to validate; {@code null} is treated as empty
   * @param maxCharacters the maximum length allowed by the storage column
   * @param rowContext    identifies the offending row, e.g. a sample name or sample id; may be
   *                      {@code null} when the caller cannot identify the row
   */
  public static ValidationResult validate(String fieldName, String value, int maxCharacters,
      String rowContext) {
    if (value == null || value.length() <= maxCharacters) {
      return ValidationResult.successful();
    }
    String row = (rowContext == null || rowContext.isBlank())
        ? ""
        : " for '" + rowContext + "'";
    return ValidationResult.withFailures(List.of(
        "%s: exceeds the maximum length of %d characters%s (got %d)"
            .formatted(fieldName, maxCharacters, row, value.length())));
  }

  /**
   * Validates the length of one field for every row of a batch upload.
   *
   * @param fieldName     the user-facing name of the field, e.g. {@code "Comment"}
   * @param maxCharacters the maximum length allowed by the storage column
   * @param valuesByRow   the field value per row, keyed by a row identifier such as the sample id
   */
  public static ValidationResult validateAll(String fieldName, int maxCharacters,
      Map<String, String> valuesByRow) {
    return valuesByRow.entrySet().stream()
        .map(entry -> validate(fieldName, entry.getValue(), maxCharacters, entry.getKey()))
        .reduce(ValidationResult.successful(), ValidationResult::combine);
  }
}
