package life.qbic.datamanager.signposting.http;

import java.util.List;
import life.qbic.datamanager.signposting.http.parsing.RawLinkHeader;

/**
 * <interface short description>
 *
 * @since <version tag>
 */
public interface Validator {

  ValidationResult validate(RawLinkHeader rawLinkHeader);

  record ValidationResult(List<WebLink> weblinks, IssueReport report) {

    public boolean containsIssues() {
      return !report.isEmpty();
    }
  }

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

  enum IssueType {
    WARNING,
    ERROR
  }
}
