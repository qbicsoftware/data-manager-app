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

    boolean containsIssues() {
      return !report.isEmpty();
    }
  }

  record IssueReport(List<Issue> issues) {

    boolean hasErrors() {
      return issues.stream().anyMatch(Issue::isError);
    }

    boolean hasWarnings() {
      return issues.stream().anyMatch(Issue::isWarning);
    }

    boolean isEmpty() {
      return issues.isEmpty();
    }
  }

  record Issue(String message, IssueType type) {

    boolean isWarning() {
      return type.equals(IssueType.WARNING);
    }

    boolean isError() {
      return type.equals(IssueType.ERROR);
    }
  }

  enum IssueType {
    WARNING,
    ERROR
  }
}
