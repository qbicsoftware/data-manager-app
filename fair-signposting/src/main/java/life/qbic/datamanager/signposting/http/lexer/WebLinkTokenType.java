package life.qbic.datamanager.signposting.http.lexer;

/**
 * Enumeration for being used to describe different token types for the
 */
public enum WebLinkTokenType {

  /**
   * "<"
   */
  LT,

  /**
   * ">"
   */
  GT,

  /**
   * ";"
   */
  SEMICOLON,

  /**
   * "="
   */
  EQUALS,

  /**
   * ","
   */
  COMMA,

  /**
   * A URI-Reference between "<" and ">". The angle brackets themselves are represented by LT and GT
   * tokens.
   */
  URI,

  /**
   * An unquoted token (e.g. parameter name, token value).
   */
  IDENT,

  /**
   * A quoted-string value without the surrounding quotes.
   */
  QUOTED,

  /**
   * End-of-input marker.
   */
  EOF
}
