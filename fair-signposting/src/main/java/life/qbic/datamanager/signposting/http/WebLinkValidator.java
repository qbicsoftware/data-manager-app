package life.qbic.datamanager.signposting.http;

import java.util.List;
import life.qbic.datamanager.signposting.http.parsing.RawLinkHeader;

/**
 * Performs validation of raw web link headers.
 * <p>
 * Validator are expected to consume output of a {@link WebLinkParser} and convert the web link
 * information into reusable web link objects.
 * <p>
 * Implementations of the {@link WebLinkValidator} interface must perform semantic validation only.
 * <p>
 * Implementations also must not interrupt the validation on violations but provide the information
 * in the attached {@link IssueReport} of the {@link ValidationResult}.
 */
public interface WebLinkValidator {

  /**
   * Validates the given raw link header against the semantic integrity of the validator type.
   * <p>
   * Violations on the semantic level must be recorded in the returned issue list with type
   * {@link IssueType#ERROR}. In the presence of any error, at least one web link entry is faulty
   * and appropriate error handling is advised.
   * <p>
   * Warnings shall indicate less strict deviations of the specification and must result in usable
   * web link objects. If no errors are provided, the client must be able to be safely continue to
   * use the web link object in the semantic scope that the validator guarantees.
   * <p>
   * The implementation MUST NOT interrupt the validation in case any error is recorded. Validation
   * shall always complete successfully and the method return the validation result.
   *
   * @param rawLinkHeader the raw link header
   * @return the validation result with a list of web link objects and an {@link IssueReport}.
   * @throws NullPointerException if the raw link header is {@code null}
   */
  ValidationResult validate(RawLinkHeader rawLinkHeader) throws NullPointerException;

  /**
   * A summary of the validation with the final web links for further use and an issue report with
   * validation warnings or violations.
   *
   * @param weblinks a collection of web links that have been converted from validation
   * @param report   a container for recorded issues during validation
   */
  record ValidationResult(List<WebLink> weblinks, IssueReport report) {

    public boolean containsIssues() {
      return !report.isEmpty();
    }
  }

  /**
   * A container for recorded issues during validation.
   *
   * @param issues the issues found during validation
   */
  record IssueReport(List<Issue> issues) {

    public boolean hasErrors() {
      return issues.stream().anyMatch(Issue::isError);
    }

    public boolean hasWarnings() {
      return issues.stream().anyMatch(Issue::isWarning);
    }

    public boolean isEmpty() {
      return issues.isEmpty();
    }
  }

  /**
   * Describes any deviations from a semantic model either as warning or error.
   *
   * @param message a descriptive message that helps clients to process the issue
   * @param type    the severity level of the issue. {@link IssueType#ERROR} shall be used to
   *                indicate serious violations from the semantic model that would lead to wrong
   *                interpretation by the client. For less severe deviations the
   *                {@link IssueType#WARNING} can be used.
   */
  record Issue(String message, IssueType type) {

    public static Issue warning(String message) {
      return new Issue(message, IssueType.WARNING);
    }

    public static Issue error(String message) {
      return new Issue(message, IssueType.ERROR);
    }

    public boolean isWarning() {
      return type.equals(IssueType.WARNING);
    }

    public boolean isError() {
      return type.equals(IssueType.ERROR);
    }
  }

  /**
   * An enumeration of different issue types.
   *
   * <ul>
   *   <li>ERROR - Deviation from the semantic level that brakes interpretation, a specification or contract</li>
   *   <li>WARNING - Deviation from the semantic level that does not brake interpretation, specification or a contract</li>
   * </ul>
   */
  enum IssueType {
    WARNING,
    ERROR
  }
}
