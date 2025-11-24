package life.qbic.datamanager.signposting.http.validation;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import life.qbic.datamanager.signposting.http.WebLinkParameter;
import life.qbic.datamanager.signposting.http.Validator;
import life.qbic.datamanager.signposting.http.WebLink;
import life.qbic.datamanager.signposting.http.parsing.RawLink;
import life.qbic.datamanager.signposting.http.parsing.RawLinkHeader;
import life.qbic.datamanager.signposting.http.parsing.RawParam;

/**
 * Validation against RFC 8288 Web Linking.
 * <p>
 * Violations against the specification will be recorded as
 * {@link life.qbic.datamanager.signposting.http.Validator.IssueType#ERROR}. In the presence of at
 * least one error, the web link MUST be regarded invalid and clients shall not continue to work
 * with the link, but treat it as exception.
 * <p>
 * The implementation also records issues as
 * {@link life.qbic.datamanager.signposting.http.Validator.IssueType#WARNING}, in case the finding
 * is not strictly against the RFC 8288, but e.g. a type usage is deprecated or when parameters have
 * been skipped when the specification demands for it. A warning results in a still usable web link,
 * but it is advised to investigate any findings.
 *
 */
public class Rfc8288Validator implements Validator {

  // Defined in https://www.rfc-editor.org/rfc/rfc7230, section 3.2.6
  private static final Pattern ALLOWED_TOKEN_CHARS = Pattern.compile(
      "^[!#$%&'*+-.^_`|~0-9A-Za-z]+$");

  @Override
  public ValidationResult validate(RawLinkHeader rawLinkHeader) {
    var recordedIssues = new ArrayList<Issue>();

    var webLinks = new ArrayList<WebLink>();
    for (RawLink rawLink : rawLinkHeader.rawLinks()) {
      var webLink = validate(rawLink, recordedIssues);
      if (webLink != null) {
        webLinks.add(webLink);
      }
    }
    return new ValidationResult(webLinks, new IssueReport(List.copyOf(recordedIssues)));
  }

  /**
   * Validation entry point for a single raw link. Any findings must be recorded in the provided
   * issue list. Only issue additions are allowed.
   * <p>
   * In case the target is not a valid URI, the returned web link is {@code null}.
   *
   * @param rawLink        the raw link information from parsing
   * @param recordedIssues a list to record negative findings as warnings and errors
   * @return a web link object, or {@code null}, in case the target is not a valid URI
   */
  private WebLink validate(RawLink rawLink, List<Issue> recordedIssues) {
    URI uri = null;
    try {
      uri = URI.create(rawLink.rawURI());
    } catch (IllegalArgumentException e) {
      recordedIssues.add(
          Issue.error("Invalid URI '%s': %s".formatted(rawLink.rawURI(), e.getMessage())));
    }
    var parameters = validateAndConvertParams(rawLink.rawParameters(), recordedIssues);

    if (uri == null) {
      return null;
    }
    return new WebLink(uri, parameters);
  }

  /**
   * Validates a list of raw parameters and creates a list of link parameters that can be used to
   * build the final web link object.
   * <p>
   * Any error or warning will be recorded in the provided recorded issue list.
   *
   * @param rawParams      a list of raw parameter values
   * @param recordedIssues a list of recorded issues to add more findings during validation
   * @return a list of converted link parameters
   */
  private List<WebLinkParameter> validateAndConvertParams(
      List<RawParam> rawParams, List<Issue> recordedIssues) {
    var params = new ArrayList<WebLinkParameter>();
    var seenParams = new HashSet<String>();
    for (RawParam rawParam : rawParams) {
      validateParam(rawParam, recordedIssues);
      validateParamOccurrenceAndAddLink(rawParam, seenParams, params, recordedIssues);
    }
    return params;
  }

  /**
   * Validates a given raw parameter against known constraints and assumptions in the RFC 8288
   * specification.
   * <p>
   * Currently, checks:
   *
   * <ul>
   *   <li>the parameter name MUST contain allowed characters only (see token definition)</li>
   * </ul>
   *
   * @param rawParam       the raw parameter to be validated
   * @param recordedIssues a list of issues to record more findings
   */
  private void validateParam(RawParam rawParam, List<Issue> recordedIssues) {
    if (tokenContainsInvalidChars(rawParam.name())) {
      recordedIssues.add(
          Issue.error("Invalid parameter name '%s': Only the characters '%s' are allowed".formatted(
              rawParam.name(), ALLOWED_TOKEN_CHARS.pattern())));
    }
  }

  /**
   * Looks for the presence of invalid chars.
   * <p>
   * Allowed token chars are defined by <a href="https://www.rfc-editor.org/rfc/rfc7230">RFC
   * 7230</a>, section 3.2.6.
   *
   * @param token the token to be checked for invalid characters
   * @return true, if the token violates the token character specification, else false
   */
  private static boolean tokenContainsInvalidChars(String token) {
    return !ALLOWED_TOKEN_CHARS.matcher(token).matches();
  }

  /**
   * Validates parameter occurrence rules and honors the RFC 8288 specification for skipping
   * parameter entries.
   * <p>
   * Sofar multiple definitions are only allowed for the "hreflang" parameter.
   * <p>
   * Note: occurrences after the first are ignored and issue a warning. This is a strict requirement
   * from the RFC 8288 and must be honored.
   *
   * @param rawParam               the raw parameter value
   * @param recordedParameterNames a set to check, if a parameter has been already seen in the link
   * @param parameters             a list of converted link parameters for the final web link
   *                               object
   * @param recordedIssues         a list of issue records to add new findings
   */
  private void validateParamOccurrenceAndAddLink(
      RawParam rawParam,
      Set<String> recordedParameterNames,
      List<WebLinkParameter> parameters,
      List<Issue> recordedIssues) {
    var rfcParamOptional = RfcLinkParameter.from(rawParam.name());

    if (rfcParamOptional.isPresent()) {
      var rfcParam = rfcParamOptional.get();
      // the "hreflang" parameter is the only parameter that is allowed to occur more than once
      // see RFC 8288 for the parameter multiplicity definition
      if (recordedParameterNames.contains(rawParam.name()) && !rfcParam.equals(
          RfcLinkParameter.HREFLANG)) {
        recordedIssues.add(Issue.warning(
            "Parameter '%s' is not allowed multiple times. Skipped parameter.".formatted(
                rfcParam.rfcValue())));
        return;
      }
    }
    recordedParameterNames.add(rawParam.name());

    WebLinkParameter webLinkParameter;
    if (rawParam.value() == null || rawParam.value().isEmpty()) {
      webLinkParameter = WebLinkParameter.createWithoutValue(rawParam.name());
    } else {
      webLinkParameter = WebLinkParameter.create(rawParam.name(), rawParam.value());
    }
    parameters.add(webLinkParameter);
  }
}
