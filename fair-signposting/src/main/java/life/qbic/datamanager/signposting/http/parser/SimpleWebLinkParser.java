package life.qbic.datamanager.signposting.http.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.datamanager.signposting.http.WebLinkParser;
import life.qbic.datamanager.signposting.http.lexer.WebLinkToken;
import life.qbic.datamanager.signposting.http.lexer.WebLinkTokenType;

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


  @Override
  public RawLinkHeader parse(List<WebLinkToken> tokens)
      throws NullPointerException, StructureException {
    Objects.requireNonNull(tokens);
    if (tokens.isEmpty()) {
      throw new StructureException(
          "A link header entry must have at least one web link. Tokens were empty.");
    }

    this.tokens = tokens.stream()
        .sorted(Comparator.comparingInt(WebLinkToken::position))
        .toList();

    ensureEOF("Lexer did not append EOF token");

    // reset the current the parser state
    currentPosition = 0;

    if (this.tokens.get(currentPosition).type() == WebLinkTokenType.EOF) {
      throw new StructureException(
          "A link header entry must have at least one web link. Tokens started with EOF.");
    }

    var collectedLinks = new ArrayList<RawLink>();

    while (this.tokens.get(currentPosition).type() != WebLinkTokenType.EOF) {
      var parsedLink = parseLinkValue();
      if (parsedLink == null) {
        throw new IllegalStateException("Should never happen");
      }
      collectedLinks.add(parsedLink);
      currentPosition++;
    }

    return new RawLinkHeader(collectedLinks);
  }

  private void ensureEOF(String errorMessage) throws IllegalStateException {
    if (tokens.getLast().type() != WebLinkTokenType.EOF) {
      throw new IllegalStateException(errorMessage);
    }
  }

  private RawLink parseLinkValue() {
    var parsedLinkValue = parseUriReference();
    var parsedLinkParameters = parseParameters();
    return new RawLink(parsedLinkValue, parsedLinkParameters);
  }

  private Map<String, RawParam> parseParameters() {

    return null;
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
          "Expected %s but found %s('%s') at position %d".formatted(token, current().type(), current().text(), current().position()));
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

  private WebLinkToken current() {
    return tokens.get(currentPosition);
  }

  private WebLinkToken next() {
    if (currentPosition == tokens.size() - 1) {
      return tokens.get(currentPosition);
    }
    return tokens.get(currentPosition++);
  }

  private WebLinkToken peek() {
    var nextPosition = currentPosition < tokens.size() - 1 ? currentPosition + 1 : currentPosition;
    return tokens.get(nextPosition);
  }
}
