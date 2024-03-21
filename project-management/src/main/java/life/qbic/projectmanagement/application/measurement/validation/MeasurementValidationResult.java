package life.qbic.projectmanagement.application.measurement.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * <b>Measurement Validation Result</b>
 *
 * <p>Validation Result in a measurement context which contains an extendable collection of Strings
 * indicating failure or warnings occurring during the measurement validation
 * </p>
 *
 */
public class MeasurementValidationResult {

  private final int validatedEntries;

  private final List<String> warnings;

  private final List<String> failures;

  private MeasurementValidationResult() {
    this.validatedEntries = 0;
    this.warnings = Collections.emptyList();
    this.failures = Collections.emptyList();
  }

  private MeasurementValidationResult(int validatedEntries) {
    this.validatedEntries = validatedEntries;
    this.warnings = Collections.emptyList();
    this.failures = Collections.emptyList();
  }

  private MeasurementValidationResult(int validatedEntries, Collection<String> warnings,
      Collection<String> failures) {
    this.validatedEntries = validatedEntries;
    this.warnings = warnings.stream().toList();
    this.failures = failures.stream().toList();
  }

  public static MeasurementValidationResult successful(int validatedEntries) {
    return new MeasurementValidationResult(validatedEntries);
  }

  public static MeasurementValidationResult withFailures(int validatedEntries,
      Collection<String> failureReports) {
    return new MeasurementValidationResult(validatedEntries, new ArrayList<>(), failureReports);
  }

  public static MeasurementValidationResult successful(int validatedEntries,
      Collection<String> warnings) {
    return new MeasurementValidationResult(validatedEntries, warnings, new ArrayList<>());
  }


  /**
   * Indicates if the validation has passed successfully without any failures.
   * <p>
   * Note: Even if the validation was successful, the validation result might still contain warnings
   * for the client, so go on and check for any warnings via
   * {@link MeasurementValidationResult#containsWarnings()}
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

  public MeasurementValidationResult combine(MeasurementValidationResult otherResult) {
    return new MeasurementValidationResult(this.validatedEntries + otherResult.validatedEntries,
        Stream.concat(this.warnings.stream(), otherResult.warnings.stream()).toList(),
        Stream.concat(this.failures.stream(), otherResult.failures.stream()).toList());
  }
}
