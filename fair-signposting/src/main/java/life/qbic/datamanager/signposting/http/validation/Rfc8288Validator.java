package life.qbic.datamanager.signposting.http.validation;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import life.qbic.datamanager.signposting.http.LinkParameter;
import life.qbic.datamanager.signposting.http.Validator;
import life.qbic.datamanager.signposting.http.WebLink;
import life.qbic.datamanager.signposting.http.parsing.RawLink;
import life.qbic.datamanager.signposting.http.parsing.RawLinkHeader;
import life.qbic.datamanager.signposting.http.parsing.RawParam;

/**
 * <class short description>
 *
 * @since <version tag>
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

  private WebLink validate(RawLink rawLink, List<Issue> recordedIssues) {
    URI uri = null;
    try {
      uri = URI.create(rawLink.rawURI());
    } catch (IllegalArgumentException e) {
      recordedIssues.add(
          Issue.error("Invalid URI '%s': %s".formatted(rawLink.rawURI(), e.getMessage())));
    }
    var parameters = validateParams(rawLink.rawParameters(), recordedIssues);

    if (uri == null) {
      return null;
    }
    return new WebLink(uri, parameters);
  }

  private List<LinkParameter> validateParams(
      List<RawParam> rawParams, List<Issue> recordedIssues) {
    var params = new ArrayList<LinkParameter>();
    var seenParams = new HashSet<String>();
    for (RawParam rawParam : rawParams) {
      validateParam(rawParam, recordedIssues);
      validateParamOccurrence(rawParam, seenParams, params, recordedIssues);
    }
    return params;
  }

  private void validateParam(RawParam rawParam, List<Issue> recordedIssues) {
    if (tokenContainsInvalidChars(rawParam.name())) {
      recordedIssues.add(
          Issue.error("Invalid parameter name '%s': Only the characters '%s' are allowed".formatted(
              rawParam.name(), ALLOWED_TOKEN_CHARS.pattern())));
    }
  }

  private static boolean tokenContainsInvalidChars(String token) {
    return !ALLOWED_TOKEN_CHARS.matcher(token).matches();
  }

  private void validateParamOccurrence(
      RawParam rawParam,
      Set<String> seenParams,
      List<LinkParameter> parameters,
      List<Issue> recordedIssues) {
    var rfcParamOptional = RfcLinkParameter.from(rawParam.name());

    if (rfcParamOptional.isPresent()) {
      var rfcParam = rfcParamOptional.get();
      if (seenParams.contains(rawParam.name()) && !rfcParam.equals(RfcLinkParameter.HREFLANG)) {
        recordedIssues.add(Issue.warning(
            "Parameter '%s' is not allowed multiple times. Skipped parameter.".formatted(rfcParam.rfcValue())));
        return;
      }
    }
    seenParams.add(rawParam.name());

    LinkParameter linkParameter;
    if (rawParam.value() == null || rawParam.value().isEmpty()) {
      linkParameter = LinkParameter.createWithoutValue(rawParam.name());
    } else {
      linkParameter = LinkParameter.create(rawParam.name(), rawParam.value());
    }
    parameters.add(linkParameter);
  }
}
