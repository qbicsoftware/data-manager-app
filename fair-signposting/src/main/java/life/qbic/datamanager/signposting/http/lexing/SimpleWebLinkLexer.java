package life.qbic.datamanager.signposting.http.lexing;

import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.signposting.http.WebLinkLexer;
import life.qbic.datamanager.signposting.http.WebLinkTokenType;

/**
 * Simple scanning lexer for RFC 8288 Web Link serialisations.
 * <p>
 * This implementation:
 * <ul>
 *   <li>Skips ASCII whitespace (OWS/BWS) between tokens</li>
 *   <li>Treats URIs as everything between "&lt;" and "&gt;"</li>
 *   <li>Treats unquoted tokens as IDENT</li>
 *   <li>Produces QUOTED tokens for quoted-string values (without the quotes)</li>
 *   <li>Emits an EOF token at the end of input</li>
 * </ul>
 * <p>
 * Parsing and semantic validation are handled by later stages.
 */
public final class SimpleWebLinkLexer implements WebLinkLexer {

  @Override
  public List<WebLinkToken> lex(String input) throws WebLinkLexingException {
    return new Scanner(input).scan();
  }

  /**
   * Internal scanner doing a single left-to-right pass over the input.
   */
  private static final class Scanner {

    private final String input;
    private final int length;
    private int pos = 0;

    private final List<WebLinkToken> tokens = new ArrayList<>();

    Scanner(String input) {
      this.input = input != null ? input : "";
      this.length = this.input.length();
    }

    List<WebLinkToken> scan() {
      while (!eof()) {
        char c = peek();

        if (isWhitespace(c)) {
          consumeWhitespace();
          continue;
        }

        int start = pos;

        switch (c) {
          case '<' -> readUri(start);
          case '>' -> {
            advance();
            tokens.add(WebLinkToken.of(WebLinkTokenType.GT, ">", start));
          }
          case ';' -> {
            advance();
            tokens.add(WebLinkToken.of(WebLinkTokenType.SEMICOLON, ";", start));
          }
          case '=' -> {
            advance();
            tokens.add(WebLinkToken.of(WebLinkTokenType.EQUALS, "=", start));
          }
          case ',' -> {
            advance();
            tokens.add(WebLinkToken.of(WebLinkTokenType.COMMA, ",", start));
          }
          case '"' -> readQuoted(start);
          default -> readIdent(start);
        }
      }

      tokens.add(WebLinkToken.of(WebLinkTokenType.EOF, "", pos));
      return tokens;
    }

    /**
     * Reads a URI-Reference between "&lt;" and "&gt;". Emits three tokens: LT, URI, GT.
     */
    private void readUri(int start) {
      // consume "<"
      advance();
      tokens.add(WebLinkToken.of(WebLinkTokenType.LT, "<", start));

      int uriStart = pos;

      while (!eof()) {
        char c = peek();
        if (c == '>') {
          break;
        }
        advance();
      }

      if (eof()) {
        throw new WebLinkLexingException(
            "Unterminated URI reference: missing '>' for '<' at position " + start);
      }

      String uriText = input.substring(uriStart, pos);
      tokens.add(WebLinkToken.of(WebLinkTokenType.URI, uriText, uriStart));

      // consume ">"
      int gtPos = pos;
      advance();
      tokens.add(WebLinkToken.of(WebLinkTokenType.GT, ">", gtPos));
    }

    /**
     * Reads a quoted-string, without including the surrounding quotes. Does not yet handle escape
     * sequences; that can be extended later.
     */
    private void readQuoted(int start) {
      // consume opening quote
      advance();

      int contentStart = pos;

      while (!eof()) {
        char c = peek();
        if (c == '"') {
          break;
        }
        // TODO: handle quoted-pair / escaping if needed
        advance();
      }

      if (eof()) {
        throw new WebLinkLexingException(
            "Unterminated quoted-string starting at position " + start);
      }

      String content = input.substring(contentStart, pos);

      // consume closing quote
      advance();

      tokens.add(WebLinkToken.of(WebLinkTokenType.QUOTED, content, contentStart));
    }

    /**
     * Reads an unquoted token (IDENT) until a delimiter or whitespace is reached.
     */
    private void readIdent(int start) {
      while (!eof()) {
        char c = peek();
        if (isDelimiter(c) || isWhitespace(c)) {
          break;
        }
        advance();
      }

      String text = input.substring(start, pos);
      if (!text.isEmpty()) {
        tokens.add(WebLinkToken.of(WebLinkTokenType.IDENT, text, start));
      }
    }

    private void consumeWhitespace() {
      while (!eof() && isWhitespace(peek())) {
        advance();
      }
    }

    private boolean isWhitespace(char c) {
      // OWS/BWS: space or horizontal tab are most important;
      // here we also accept CR/LF defensively.
      return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    /**
     * Characters that delimit IDENT tokens.
     */
    private boolean isDelimiter(char c) {
      return switch (c) {
        case '<', '>', ';', '=', ',', '"' -> true;
        default -> false;
      };
    }

    private boolean eof() {
      return pos >= length;
    }

    private char peek() {
      return input.charAt(pos);
    }

    private void advance() {
      pos++;
    }
  }
}
