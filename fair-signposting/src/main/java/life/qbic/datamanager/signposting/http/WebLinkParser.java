package life.qbic.datamanager.signposting.http;

import java.util.Objects;

/**
 * Parses serialized information used in Web Linking as described in <a
 * href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a>.
 * <p>
 * The implementation is based on the <i>Link Serialisation in HTTP Headers</i>, section 3 of the
 * RFC 8288.
 *
 * @author sven1103
 */
public class WebLinkParser {

  private WebLinkParser() {}

  public static WebLinkParser create() {
    return new WebLinkParser();
  }

  public WebLink parse(String link) throws NullPointerException, FormatException {
    Objects.requireNonNull(link);
    throw new RuntimeException("Not implemented yet");
  }

}
