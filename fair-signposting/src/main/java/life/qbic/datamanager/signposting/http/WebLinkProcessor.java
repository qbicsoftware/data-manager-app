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
 * Configurable processor for raw web link strings from the HTTP Link header field.
 * <p>
 * The underlying standard is RFC 8288
 *
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

  /**
   * Processes a raw link header string and returns a validation result with the final web links and
   * an issue report.
   * <p>
   * The processor performs different steps until the validation result returns:
   *
   * <ol>
   *   <li>Tokenization: the raw string gets translated into enumerated token values</li>
   *   <li>Parsing: the token collection gets structurally parsed and checked, the result is an AST of raw link values</li>
   *   <li>Validation: one or more validation steps to semantically check the raw web links</li>
   * </ol>
   * <p>
   * The caller is advised to check the {@link ValidationResult#report()} in case issues have been recorded.
   * <p>
   * By contract of the validation interface, validators MUST record issues as errors in case there are severe semantically
   * deviations from the model the validator represents. Warnings can be investigated, but clients
   * can expect to continue to use the returned web links.
   *
   * @param rawLinkHeader the serialized raw link header value
   * @return a validation result with the web links and an issue report with recorded findings of
   * warnings and errors.
   * @throws LexingException      in case the header contains invalid characters (during
   *                              tokenizing)
   * @throws StructureException   in case the header does not have the expected structure (during
   *                              parsing)
   * @throws NullPointerException in case the raw link header is {@code null}
   */
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

  /**
   * Builder for a {@link WebLinkProcessor}.
   * <p>
   * The builder allows for flexible configuration of the different processing steps:
   *
   * <ol>
   *   <li>Tokenization: the raw string gets translated into enumerated token values</li>
   *   <li>Parsing: the token collection gets structurally parsed and checked, the result is an AST of raw link values</li>
   *   <li>Validation: one or more validation steps to semantically check the raw web links</li>
   * </ol>
   * <p>
   * It is possible to create a default processor by simply omitting any configuration:
   *
   * <pre>
   *   {@code
   *   // Creates a processor with default configuration
   *   WebLinkProcessor defaultProcessor = new Builder.build()
   *   }
   * </pre>
   * <p>
   * The default components are:
   *
   * <ul>
   *   <li>lexer: {@link SimpleWebLinkLexer}</li>
   *   <li>parser: {@link SimpleWebLinkParser}</li>
   *   <li>validator: {@link Rfc8288WebLinkValidator}</li>
   * </ul>
   *
   * The RFC 8282 validator will only be used if no validator has been provided. If you want
   * to combine the RFC validator with additional ones, you can do so:
   *
   * <pre>
   *   {@code
   *
   *   WebLinkProcessor customProcessor =
   *      new Builder.withValidator(Rfc8288WebLinkValidator.create())
   *                 .withValidator(new MyCustomValidator())
   *                 .build()
   *   }
   * </pre>
   */
  public static class Builder {

    private WebLinkLexer configuredLexer;

    private WebLinkParser configuredParser;

    private final List<WebLinkValidator> configuredValidators = new ArrayList<>();

    /**
     * Configures a different lexer from the default that shall be used in the processing.
     *
     * @param lexer the lexer to be used in the processing
     * @return the builder instance
     */
    public Builder withLexer(WebLinkLexer lexer) {
      configuredLexer = lexer;
      return this;
    }

    /**
     * Configures a different lexer from the default that shall be used in the processing.
     *
     * @param lexer the lexer to be used in the processing
     * @return the builder instance
     */
    public Builder withParser(WebLinkParser parser) {
      configuredParser = parser;
      return this;
    }

    /**
     * Configures a different lexer from the default that shall be used in the processing.
     * <p>
     * Multiple validators can be configured by calling this method repeatedly. The validators are
     * called in the order they have been configured on the builder.
     *
     * <pre>
     *   {@code
     *   var processor = Builder.withValidator(first)  // first validator
     *                          .withValidator(other)  // appends next validator
     *                          .build()
     *   }
     * </pre>
     *
     * @param validator the validator to be used in the processing
     * @return the builder instance
     */
    public Builder withValidator(WebLinkValidator validator) {
      configuredValidators.add(validator);
      return this;
    }

    /**
     * Creates instance of a web link processor object based on the configuration.
     *
     * @return the configured web link processor
     */
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
