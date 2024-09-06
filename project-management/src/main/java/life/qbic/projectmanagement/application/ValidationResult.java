package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * <b>Validation Result</b>
 *
 * <p>Validation Result in a measurement context which contains an extendable collection of Strings
 * indicating failure or warnings occurring during the measurement validation
 * </p>
 *
 */
public class ValidationResult {

  private final int validatedEntries;

  private final List<String> warnings;

  private final List<String> failures;

  private ValidationResult() {
    this.validatedEntries = 0;
    this.warnings = Collections.emptyList();
    this.failures = Collections.emptyList();
  }

  private ValidationResult(int validatedEntries) {
    this.validatedEntries = validatedEntries;
    this.warnings = Collections.emptyList();
    this.failures = Collections.emptyList();
  }

  private ValidationResult(int validatedEntries, Collection<String> warnings,
      Collection<String> failures) {
    this.validatedEntries = validatedEntries;
    this.warnings = warnings.stream().toList();
    this.failures = failures.stream().toList();
  }

  public static ValidationResult successful(int validatedEntries) {
    return new ValidationResult(validatedEntries);
  }

  public static ValidationResult withFailures(int validatedEntries,
      Collection<String> failureReports) {
    return new ValidationResult(validatedEntries, new ArrayList<>(), failureReports);
  }

  public static ValidationResult successful(int validatedEntries,
      Collection<String> warnings) {
    return new ValidationResult(validatedEntries, warnings, new ArrayList<>());
  }


  /**
   * Indicates if the validation has passed successfully without any failures.
   * <p>
   * Note: Even if the validation was successful, the validation result might still contain warnings
   * for the client, so go on and check for any warnings via
   * {@link ValidationResult#containsWarnings()}
   *
   * @return true, if there are no reported failures during validation, else returns false
   * @since 1.0.0
   */
  public boolean allPassed() {
    // We consider warnings as not part of a reason to fail validation
    return this.failures.isEmpty();
  }

  public boolean containsFailures() {
    return !failures.isEmpty();
  }

  public Collection<String> failures() {
    return failures.stream().toList();
  }

  public int failedEntries() {
    return failures.size();
  }

  public int validatedEntries() {
    return validatedEntries;
  }

  public boolean containsWarnings() {
    return !warnings.isEmpty();
  }

  public ValidationResult combine(ValidationResult otherResult) {
    return new ValidationResult(this.validatedEntries + otherResult.validatedEntries,
        Stream.concat(this.warnings.stream(), otherResult.warnings.stream()).toList(),
        Stream.concat(this.failures.stream(), otherResult.failures.stream()).toList());
  }
}
