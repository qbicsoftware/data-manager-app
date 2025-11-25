package life.qbic.datamanager.signposting.http;

import java.util.List;
import life.qbic.datamanager.signposting.http.lexing.WebLinkToken;

/**
 * Lexes a single Web Link (RFC 8288) serialisation string into a list of tokens.
 * <p>
 * Implementations should be stateless or thread-confined.
 */
public interface WebLinkLexer {

  /**
   * Lex the given input string into a sequence of tokens.
   *
   * @param input the raw Link header field-value or link-value
   * @return list of tokens ending with an EOF token
   * @throws WebLinkLexingException if the input is not lexically well-formed
   */
  List<WebLinkToken> lex(String input) throws WebLinkLexingException;

  /**
   * Thrown when the input cannot be tokenised according to the Web Link lexical rules.
   */
  class WebLinkLexingException extends RuntimeException {

    public WebLinkLexingException(String message) {
      super(message);
    }

    public WebLinkLexingException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
