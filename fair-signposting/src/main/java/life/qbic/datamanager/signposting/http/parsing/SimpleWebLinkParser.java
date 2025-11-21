package life.qbic.datamanager.signposting.http.parsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.signposting.http.WebLinkParser;
import life.qbic.datamanager.signposting.http.lexing.WebLinkToken;
import life.qbic.datamanager.signposting.http.lexing.WebLinkTokenType;

/**
 * Parses serialized information used in Web Linking as described in <a
 * href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a>.
 * <p>
 * The implementation is based on the <i>Link Serialisation in HTTP Headers</i>, section 3 of the
 * RFC 8288.
 *
 * <p>
 * <code>
 * Link       = #link-value <br> link-value = "<" URI-Reference ">" *( OWS ";" OWS link-param ) <br>
 * link-param = token BWS [ "=" BWS ( token / quoted-string ) ]
 * </code>
 *
 */
public class SimpleWebLinkParser implements WebLinkParser {

  private int currentPosition = 0;

  private List<WebLinkToken> tokens;

  private SimpleWebLinkParser() {
  }

  /**
   * Creates a new SimpleWebLinkParser object instance.
   *
   * @return the new SimpleWebLinkParser
   */
  public static SimpleWebLinkParser create() {
    return new SimpleWebLinkParser();
  }


  /**
   * Parses a list of lexed web link tokens to a raw link header value. The parser only performs
   * structural validation, not semantic validation.
   * <p>
   * The template for structural validation is the serialisation description in ABNF for RFC 8288
   * Section 3.
   *
   * <p>
   * Parser contract:
   *
   * <ul>
   *   <li>The token list must contain an EOF token</li>
   *   <li>The last token item must be an EOF token, based on ascending sorting by position</li>
   * </ul>
   * <p>
   * In case the contract is violated, a structure exception is thrown.
   *
   * @param tokens a list of tokens to parse as raw web link header
   * @return a raw web link header, structurally validated against RFC 8288
   * @throws NullPointerException if the tokens list is {@code null}
   * @throws StructureException   if the tokens violate the structure of a valid web link token
   */
  @Override
  public RawLinkHeader parse(List<WebLinkToken> tokens)
      throws NullPointerException, StructureException {
    Objects.requireNonNull(tokens);

    if (tokens.isEmpty()) {
      throw new StructureException(
          "A link header entry must have at least one web link. Tokens were empty.");
    }

    // Always reset the internal state on every parse() call
    reset();

    this.tokens = tokens.stream()
        .sorted(Comparator.comparingInt(WebLinkToken::position))
        .toList();

    // Validate contract
    ensureEOF("Lexer did not append EOF token");

    if (this.tokens.get(currentPosition).type() == WebLinkTokenType.EOF) {
      throw new StructureException(
          "A link header entry must have at least one web link. Tokens started with EOF.");
    }

    var collectedLinks = new ArrayList<RawLink>();

    var parsedLink = parseLinkValue();
    collectedLinks.add(parsedLink);
    // While there is ',' (COMMA) present, parse another link value
    while (current().type() == WebLinkTokenType.COMMA) {
      next();
      if (currentIsEof()) {
        throw new StructureException(
            "Unexpected trailing comma: expected another link-value after ','.");
      }
      collectedLinks.add(parseLinkValue());
    }

    // Last consumed token must be always EOF to ensure that the token stream has been consumed
    expectCurrent(WebLinkTokenType.EOF);

    return new RawLinkHeader(collectedLinks);
  }

  /**
   * Resets the internal state of the parser instance
   */
  private void reset() {
    currentPosition = 0;
  }

  /**
   * Checks if the last token in the token list is an EOF token. To keep the parser robust and
   * simple, this is part of the contract and the parser shall fail early if the contract is
   * violated.
   *
   * @param errorMessage the message to provide in the exception
   * @throws IllegalStateException if the last token of the list ist not an EOF token
   */
  private void ensureEOF(String errorMessage) throws IllegalStateException {
    if (tokens.getLast().type() != WebLinkTokenType.EOF) {
      throw new IllegalStateException(errorMessage);
    }
  }

  /**
   * Parses a single web link value, which must contain a target (URI). Optionally, the web link can
   * have one or more parameters.
   * <p>
   * If the target has a trailing ',' (COMMA), no further parameters are expected.
   * <p>
   * The correctness of the parameter structure with a precedent ';' (SEMICOLON) after the target is
   * concern of the {@link #parseParameters()} method, since it is part of the parameter list
   * description.
   *
   * @return a raw web link value with target and optionally one or more parameters
   */
  private RawLink parseLinkValue() {
    var parsedLinkValue = parseUriReference();
    if (current().type() != WebLinkTokenType.COMMA) {
      return new RawLink(parsedLinkValue, parseParameters());
    }
    return new RawLink(parsedLinkValue, List.of());
  }

  /**
   * Parses parameters beginning from the current token position (inclusive).
   * <p>
   * Based on the serialisation description of RFC 8288 for link-values, params must have a
   * precedent ';' (SEMICOLON). If the start position on method call is not a semicolon, an
   * exception will be thrown.
   * <p>
   * In case the link-value has no parameters at all (e.g. multiple web links with targets (URI)
   * only), this method should not be called in the first place.
   *
   * @return a list of raw parameters with param name and value
   */
  private List<RawParam> parseParameters() {
    var parameters = new ArrayList<RawParam>();
    if (currentIsEof()) {
      return parameters;
    }
    // expected separator for a parameter entry is ';' (semicolon) based on RFC 8288 section 3
    expectCurrent(WebLinkTokenType.SEMICOLON);
    next();

    // now one or more parameters can follow
    while (current().type() != WebLinkTokenType.COMMA) {
      RawParam parameter = parseParameter();
      parameters.add(parameter);
      // If the current token is no ';' (SEMICOLON), no additional parameters are expected
      if (current().type() != WebLinkTokenType.SEMICOLON) {
        break;
      }
      next();
    }
    return parameters;
  }

  private RawParam parseParameter() throws StructureException {
    expectCurrent(WebLinkTokenType.IDENT);
    var paramName = current().text();

    next();

    // Checks for empty parameter
    if (currentIsEof()
        || current().type() == WebLinkTokenType.COMMA
        || current().type() == WebLinkTokenType.SEMICOLON
    ) {
      return RawParam.emptyParameter(paramName);
    }

    // Next token must be "=" (equals)
    // RFC 8288: token BWS [ "=" BWS (token / quoted-string ) ]
    expectCurrent(WebLinkTokenType.EQUALS);

    next();

    expectCurrentAny(WebLinkTokenType.IDENT, WebLinkTokenType.QUOTED);
    var rawParamValue = current().text();

    next();

    return RawParam.withValue(paramName, rawParamValue);
  }

  /**
   * Evaluates if the current token is an EOF token.
   *
   * @return {@code true}, if the current token is an EOF token, else {@code false}
   */
  private boolean currentIsEof() {
    return current().type() == WebLinkTokenType.EOF;
  }

  /**
   * Checks the current token and throws an exception, if it is not of the expected type.
   *
   * @param token the expected token
   * @throws StructureException if the current token does not match the expected one
   */
  private void expectCurrent(WebLinkTokenType token) throws StructureException {
    if (current().type() != token) {
      throw new StructureException(
          "Expected %s but found %s('%s') at position %d".formatted(token, current().type(),
              current().text(), current().position()));
    }
  }

  /**
   * Checks if the current token matches any (at least one) expected token.
   * <p>
   * If no expected type is provided, the method will throw a
   * {@link life.qbic.datamanager.signposting.http.WebLinkParser.StructureException}.
   *
   * @param expected zero or more expected token types.
   * @throws StructureException if the current token does not match any expected token
   */
  private void expectCurrentAny(WebLinkTokenType... expected) throws StructureException {
    var matches = Arrays.stream(expected)
        .anyMatch(type -> type.equals(current().type()));

    if (!matches) {
      var expectedNames = Arrays.stream(expected)
          .map(Enum::name)
          .reduce((a, b) -> a + ", " + b)
          .orElse("");
      throw new StructureException(
          "Expected any of [%s] but found %s('%s') at position %d"
              .formatted(expectedNames, current().type(), current().text(), current().position()));
    }
  }

  /**
   * Will use the token from the current position with {@link this#current()} and try to parse the
   * raw URI value. After successful return the current position is advanced to the next token in
   * the list.
   *
   * @return the raw value of the URI
   */
  private String parseUriReference() {
    var uriValue = "";

    // URI value must start with '<'
    expectCurrent(WebLinkTokenType.LT);
    next();

    // URI reference expected
    expectCurrent(WebLinkTokenType.URI);
    uriValue = current().text();
    next();

    // URI value must end with '>'
    expectCurrent(WebLinkTokenType.GT);

    next();
    return uriValue;
  }

  /**
   * Returns the token on the current position.
   *
   * @return the token on the current position.
   */
  private WebLinkToken current() {
    return tokens.get(currentPosition);
  }

  /**
   * Returns the next token from the current position. If the current position is already the last
   * token of the token list, the last token will be returned.
   * <p>
   * By contract, the parser expects the last item to be an EOF token (see
   * {@link WebLinkTokenType#EOF}). So the last item in the token list will always be an EOF token.
   */
  private WebLinkToken next() {
    if (currentPosition < tokens.size() - 1) {
      currentPosition++;
    }
    return current();
  }
}
