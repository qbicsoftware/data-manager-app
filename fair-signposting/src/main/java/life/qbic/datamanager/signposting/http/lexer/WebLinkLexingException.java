package life.qbic.datamanager.signposting.http.lexer;


/**
 * Thrown when the input cannot be tokenised according to the Web Link lexical rules.
 */
public class WebLinkLexingException extends RuntimeException {

  public WebLinkLexingException(String message) {
    super(message);
  }

  public WebLinkLexingException(String message, Throwable cause) {
    super(message, cause);
  }
}
