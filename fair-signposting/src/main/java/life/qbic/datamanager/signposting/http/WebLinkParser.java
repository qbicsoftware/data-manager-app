package life.qbic.datamanager.signposting.http;

import java.util.List;
import life.qbic.datamanager.signposting.http.lexer.WebLinkToken;
import life.qbic.datamanager.signposting.http.parser.RawLinkHeader;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface WebLinkParser {

  RawLinkHeader parse(List<WebLinkToken> tokens) throws NullPointerException, StructureException;

  class StructureException extends RuntimeException {

    public StructureException(String message) {
      super(message);
    }

  }
}
