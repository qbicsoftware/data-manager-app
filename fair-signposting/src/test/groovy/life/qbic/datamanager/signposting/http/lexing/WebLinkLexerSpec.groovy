package life.qbic.datamanager.signposting.http.lexing


import life.qbic.datamanager.signposting.http.WebLinkLexer
import life.qbic.datamanager.signposting.http.WebLinkLexer.LexingException
import life.qbic.datamanager.signposting.http.WebLinkTokenType;
import spock.lang.Specification

/**
 * Specification for a {@link WebLinkLexer} implementation.
 *
 * These tests verify that a raw Web Link (RFC 8288) serialisation
 * is correctly tokenised into a sequence of {@link WebLinkToken}s,
 * ending with an EOF token, and that malformed input causes a
 * {@link LexingException}.
 *
 */
class WebLinkLexerSpec extends Specification {

  // Adjust to your concrete implementation
  WebLinkLexer lexer = new SimpleWebLinkLexer()

  /**
   * Minimal working example: just a URI reference in angle brackets.
   *
   * ABNF: link-value = "<" URI-Reference ">" *( ...)
   */
  def "lexes minimal link with URI only"() {
    given:
    def input = "<https://example.org/resource>"

    when:
    def tokens = lexer.lex(input)

    then: "token sequence matches < URI > EOF"
    tokens*.type() == [
    WebLinkTokenType.LT,
        WebLinkTokenType.URI,
        WebLinkTokenType.GT,
        WebLinkTokenType.EOF
    ]

    and: "URI token text is the raw reference"
    tokens[1].text() == "https://example.org/resource"
  }

  /**
   * Single parameter with a token value.
   *
   * Example: <https://example.org>; rel=self
   */
  def "lexes link with single token parameter"() {
    given:
    def input = "<https://example.org>; rel=self"

    when:
    def tokens = lexer.lex(input)

    then:
    tokens*.type() == [
    WebLinkTokenType.LT,
        WebLinkTokenType.URI,
        WebLinkTokenType.GT,
        WebLinkTokenType.SEMICOLON,
        WebLinkTokenType.IDENT,     // rel
        WebLinkTokenType.EQUALS,
        WebLinkTokenType.IDENT,     // self
        WebLinkTokenType.EOF
    ]

    and:
    tokens[1].text() == "https://example.org"
    tokens[4].text() == "rel"
    tokens[6].text() == "self"
  }

  /**
   * Single parameter with a quoted-string value.
   *
   * Example: <https://example.org>; title="A title"
   */
  def "lexes link with quoted-string parameter value"() {
    given:
    def input = '<https://example.org>; title="A title"'

    when:
    def tokens = lexer.lex(input)

    then:
    tokens*.type() == [
    WebLinkTokenType.LT,
        WebLinkTokenType.URI,
        WebLinkTokenType.GT,
        WebLinkTokenType.SEMICOLON,
        WebLinkTokenType.IDENT,     // title
        WebLinkTokenType.EQUALS,
        WebLinkTokenType.QUOTED,    // "A title"
        WebLinkTokenType.EOF
    ]

    and: "quoted token text does not contain quotes"
    tokens[6].text() == "A title"
  }

  /**
   * Empty quoted-string is valid: title="".
   *
   * RFC 7230 §3.2.6 allows zero-length quoted-string.
   */
  def "lexes parameter with empty quoted-string value"() {
    given:
    def input = '<https://example.org>; title=""'

    when:
    def tokens = lexer.lex(input)

    then:
    tokens*.type() == [
    WebLinkTokenType.LT,
        WebLinkTokenType.URI,
        WebLinkTokenType.GT,
        WebLinkTokenType.SEMICOLON,
        WebLinkTokenType.IDENT,     // title
        WebLinkTokenType.EQUALS,
        WebLinkTokenType.QUOTED,    // ""
        WebLinkTokenType.EOF
    ]

    and:
    tokens[6].text() == ""
  }

  /**
   * Whitespace (OWS/BWS) must be allowed around separators and '='.
   *
   * Example: <...>  ;  rel = "self"
   */
  def "ignores optional whitespace around separators and equals"() {
    given:
    def input = '<https://example.org/resource>  ;  rel = "self"  '

    when:
    def tokens = lexer.lex(input)

    then: "same token sequence as without whitespace"
    tokens*.type() == [
    WebLinkTokenType.LT,
        WebLinkTokenType.URI,
        WebLinkTokenType.GT,
        WebLinkTokenType.SEMICOLON,
        WebLinkTokenType.IDENT,
        WebLinkTokenType.EQUALS,
        WebLinkTokenType.QUOTED,
        WebLinkTokenType.EOF
    ]

    and:
    tokens[4].text() == "rel"
    tokens[6].text() == "self"
  }

  /**
   * Multiple link-values separated by a comma at the header field level.
   *
   * Example: <a>; rel=self, <b>; rel=next
   *
   * The lexer should emit a COMMA token between the two link-values.
   */
  def "lexes multiple link-values separated by comma"() {
    given:
    def input = '<https://example.org/a>; rel=self, <https://example.org/b>; rel=next'

    when:
    def tokens = lexer.lex(input)

    then:
    tokens*.type() == [
    WebLinkTokenType.LT,
        WebLinkTokenType.URI,
        WebLinkTokenType.GT,
        WebLinkTokenType.SEMICOLON,
        WebLinkTokenType.IDENT,
        WebLinkTokenType.EQUALS,
        WebLinkTokenType.IDENT,
        WebLinkTokenType.COMMA,
        WebLinkTokenType.LT,
        WebLinkTokenType.URI,
        WebLinkTokenType.GT,
        WebLinkTokenType.SEMICOLON,
        WebLinkTokenType.IDENT,
        WebLinkTokenType.EQUALS,
        WebLinkTokenType.IDENT,
        WebLinkTokenType.EOF
    ]

    and:
    tokens[1].text() == "https://example.org/a"
    tokens[6].text() == "self"
    tokens[9].text() == "https://example.org/b"
    tokens[14].text() == "next"
  }

  /**
   * Unterminated quoted-string should be rejected by the lexer.
   *
   * Example: title="unterminated
   */
  def "throws on unterminated quoted string"() {
    given:
    def input = '<https://example.org>; title="unterminated'

    when:
    lexer.lex(input)

    then:
    thrown(LexingException)
  }

  /**
   * Unterminated URI reference (missing closing '>') should be rejected.
   *
   * Example: <https://example.org
   */
  def "throws on unterminated URI reference"() {
    given:
    def input = '<https://example.org'

    when:
    lexer.lex(input)

    then:
    thrown(LexingException)
  }
}
