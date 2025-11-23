package life.qbic.datamanager.signposting.http;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A Java record representing a web link object following the
 * <a href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a> model specification.
 */
public record WebLink(URI reference, List<LinkParameter> params) {

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
   * @throws FormatException      if the parameters violate any known specification described in the
   *                              RFC
   * @throws NullPointerException if any method argument is {@code null}
   */
  public static WebLink create(URI reference, List<LinkParameter> params)
      throws FormatException, NullPointerException {
    Objects.requireNonNull(reference);
    Objects.requireNonNull(params);
    return new WebLink(reference, params);
  }

  /**
   * Web link constructor that can be used if a web link has no parameters.
   * <p>
   *
   * @param reference a {@link URI} pointing to the actual resource
   * @return the new Weblink
   * @throws FormatException      if the parameters violate any known specification described in the
   *                              RFC
   * @throws NullPointerException if any method argument is {@code null}
   */
  public static WebLink create(URI reference) throws FormatException, NullPointerException {
    return create(reference, List.of());
  }

  /**
   * Returns all "rel" parameter values of the link.
   * <p>
   * RFC 8288 section 3.3 states, that the relation parameter MUST NOT appear more than once in a
   * given link-value, but one "rel" parameter value can contain multiple relation-types when
   * separated by one or more space characters (SP = ASCII 0x20):
   * <p>
   * {@code relation-type *( 1*SP relation-type ) }.
   * <p>
   * The method returns space-separated values as individual values of the "rel" parameter.
   *
   * @return a list of relation parameter values
   */
  public List<String> relations() {
    return List.of();
  }


}
