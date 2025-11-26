package life.qbic.datamanager.signposting.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.signposting.http.WebLinkLexer.LexingException;
import life.qbic.datamanager.signposting.http.WebLinkParser.StructureException;
import life.qbic.datamanager.signposting.http.WebLinkValidator.Issue;
import life.qbic.datamanager.signposting.http.WebLinkValidator.IssueReport;
import life.qbic.datamanager.signposting.http.WebLinkValidator.ValidationResult;
import life.qbic.datamanager.signposting.http.lexing.SimpleWebLinkLexer;
import life.qbic.datamanager.signposting.http.parsing.SimpleWebLinkParser;
import life.qbic.datamanager.signposting.http.validation.Rfc8288WebLinkValidator;

/**
 * <class short description>
 *
 * @since <version tag>
 */
public class WebLinkProcessor {

  private final WebLinkLexer lexer;
  private final WebLinkParser parser;
  private final List<WebLinkValidator> validators;

  private WebLinkProcessor() {
    this.lexer = null;
    this.parser = null;
    this.validators = null;
  }

  private WebLinkProcessor(
      WebLinkLexer selectedLexer,
      WebLinkParser selectedParser,
      List<WebLinkValidator> selectedValidators) {
    this.lexer = Objects.requireNonNull(selectedLexer);
    this.parser = Objects.requireNonNull(selectedParser);
    this.validators = List.copyOf(Objects.requireNonNull(selectedValidators));
  }

  public ValidationResult process(String rawLinkHeader)
      throws LexingException, StructureException, NullPointerException {
    var header = Objects.requireNonNull(rawLinkHeader);
    var tokenizedHeader = lexer.lex(header);
    var parsedHeader = parser.parse(tokenizedHeader);

    var aggregatedIssues = new ArrayList<Issue>();
    ValidationResult cachedValidationResult = null;
    for (WebLinkValidator validator : validators) {
      cachedValidationResult = validator.validate(parsedHeader);
      aggregatedIssues.addAll(cachedValidationResult.report().issues());
    }

    if (cachedValidationResult == null) {
      throw new IllegalStateException(
          "No validation result was found after processing: " + rawLinkHeader);
    }

    return new ValidationResult(cachedValidationResult.weblinks(),
        new IssueReport(aggregatedIssues));
  }

  public static class Builder {

    private WebLinkLexer configuredLexer;

    private WebLinkParser configuredParser;

    private final List<WebLinkValidator> configuredValidators = new ArrayList<>();

    public Builder withLexer(WebLinkLexer lexer) {
      configuredLexer = lexer;
      return this;
    }

    public Builder withParser(WebLinkParser parser) {
      configuredParser = parser;
      return this;
    }

    public Builder withValidator(WebLinkValidator validator) {
      configuredValidators.add(validator);
      return this;
    }

    public WebLinkProcessor build() {
      var selectedLexer = configuredLexer == null ? defaultLexer() : configuredLexer;
      var selectedParser = configuredParser == null ? defaultParser() : configuredParser;
      var selectedValidators =
          configuredValidators.isEmpty() ? List.of(defaultValidator()) : configuredValidators;

      return new WebLinkProcessor(selectedLexer, selectedParser, selectedValidators);
    }

    private WebLinkParser defaultParser() {
      return SimpleWebLinkParser.create();
    }

    private static WebLinkLexer defaultLexer() {
      return SimpleWebLinkLexer.create();
    }

    private static WebLinkValidator defaultValidator() {
      return Rfc8288WebLinkValidator.create();
    }
  }
}
