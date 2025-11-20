package life.qbic.datamanager.signposting.http.lexer;

/**
 * Single token produced by a WebLinkLexer.
 *
 * @param type     the token type
 * @param text     the raw text content for this token (without decorations like quotes)
 * @param position the zero-based character offset in the input where this token starts
 */
public record WebLinkToken(
    WebLinkTokenType type,
    String text,
    int position
) {

  public static WebLinkToken of(WebLinkTokenType type, String text, int position) {
    return new WebLinkToken(type, text, position);
  }

  @Override
  public String toString() {
    return type + "('" + text + "' @" + position + ")";
  }
}
