package life.qbic.datamanager.signposting.http;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A Java record representing a web link object following the
 * <a href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a> model specification.
 *
 * @author sven1103
 */
public record WebLink(URI reference, Map<String, List<String>> params) {

  /**
   * Creates an <a href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a> compliant web
   * link object.
   * <p>
   * Following RFC8288, the ABNF for a link parameter is:
   * <p>
   * {@code link-param = token BWS [ "=" BWS ( token / quoted-string ) ]}
   * <p>
   * The parameter key must not be empty, so during construction the {@code params} keys are checked
   * for an empty key. The values can be empty though.
   *
   * @param reference a {@link URI} pointing to the actual resource
   * @param params    a {@link Map} of parameters as keys and a list of their values
   * @return the new Weblink
   * @throws FormatException if the parameters violate any known specification described in the RFC
   * @throws NullPointerException if any method argument is {@code null}
   */
  public static WebLink create(URI reference, Map<String, List<String>> params)
      throws FormatException, NullPointerException {
    Objects.requireNonNull(reference);
    Objects.requireNonNull(params);
    if (hasEmptyParameterKey(params)) {
      throw new FormatException("A parameter key must not be empty");
    }
    return new WebLink(reference, params);
  }

  /**
   * Web link constructor that can be used if a web link has no parameters.
   * <p>
   * See {@link WebLink#create(URI, Map)} for the full description.
   *
   * @param reference a {@link URI} pointing to the actual resource
   * @return the new Weblink
   * @throws FormatException if the parameters violate any known specification described in the RFC
   * @throws NullPointerException if any method argument is {@code null}
   */
  public static WebLink create(URI reference) throws FormatException, NullPointerException {
    return create(reference, new HashMap<>());
  }

  /**
   * Verifies the {@code token} has at least one character or more.
   * <p>
   * See <a href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc7230#section-3.2.6">RFC 7230 section 3.2.6</a>:
   * <p>
   * {@code link-param = token BWS [ "=" BWS ( token / quoted-string ) ]}
   * <p>
   * The parameter key must not be empty, so during construction the {@code params} keys are checked
   * for an empty key. The values can be empty though.
   *
   * @param params the parameter map to check for an empty parameter key
   * @return {@code true}, if an empty parameter key exists, else {@code false}
   */
  private static boolean hasEmptyParameterKey(Map<String, List<String>> params) {
    return params.containsKey("");
  }

}
