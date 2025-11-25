package life.qbic.datamanager.signposting.http;

import java.util.List;
import life.qbic.datamanager.signposting.http.lexing.WebLinkToken;
import life.qbic.datamanager.signposting.http.parsing.RawLinkHeader;

/**
 * A parser that checks structural integrity of an HTTP Link header entry in compliance with <a
 * href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a>.
 * <p>
 * A web link parser is able to process tokens from web link lexing and convert the tokens to raw
 * link headers after structural validation, which can be seen as an AST (abstract syntax tree).
 * <p>
 * Note: Implementations <strong>must not</strong> perform semantic validation, this is concern of
 * {@link WebLinkValidator} implementations.
 * <p>
 * In case of structural violations, implementations of the {@link WebLinkParser} interface must
 * throw a {@link StructureException}.
 * <p>
 * RFC 8288 section 3 describes the serialization of the Link HTTP header attribute:
 *
 * <pre>
 *   {@code
 *   Link       = #link-value
 *   link-value = "<" URI-Reference ">" *( OWS ";" OWS link-param )
 *   link-param = token BWS [ "=" BWS ( token / quoted-string ) ]
 *   }
 * </pre>
 * <p>
 * The {@link WebLinkParser} interface can process {@link WebLinkToken}, which are the output of
 * lexing raw character values into known token values. See {@link WebLinkLexer} for details to
 * lexers.
 */
public interface WebLinkParser {

  /**
   * Parses a list of {@link WebLinkToken} and performs structural validation based on the RFC 8288
   * serialisation requirement.
   * <p>
   * The returned value is an AST of a raw link header with a list of raw web link items that can be
   * used for semantic validation.
   *
   * @param tokens a list of web link tokens to process
   * @return a raw link header parsed from the web link tokens
   * @throws NullPointerException if the token list is {@code null}
   * @throws StructureException   if any structural violation occurred
   */
  RawLinkHeader parse(List<WebLinkToken> tokens) throws NullPointerException, StructureException;

  /**
   * Indicates a structural violation of the RFC 8288 web link serialisation requirement.
   */
  class StructureException extends RuntimeException {

    public StructureException(String message) {
      super(message);
    }
  }
}
