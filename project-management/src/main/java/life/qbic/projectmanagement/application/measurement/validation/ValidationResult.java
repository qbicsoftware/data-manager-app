package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Collection;
import java.util.Collections;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ValidationResult {

  public ValidationResult() {

  }

  public boolean allPassed() {
    return false;
  }

  public boolean containsFailures() {
    return false;
  }

  public Collection<String> failures() {
    return Collections.emptyList();
  }

  public int failedEntries() {
    return -1;
  }

  public int validatedEntries() {
    return -1;
  }

  enum Status {
    PASSED, FAILED
  }

}
