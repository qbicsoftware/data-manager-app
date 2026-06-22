package life.qbic.datamanager.signposting.http;

/**
 * A parameter for the HTTP Link header attribute.
 * <p>
 * Based on RFC 8288, a parameter with only a name is valid.
 * <p>
 * <pre>
 * {@code
 * // ABNF notation for web links
 * Link = #link-value
 * link-value = "<" URI-Reference ">" *( OWS ";" OWS link-param )
 * link-param = token BWS [ "=" BWS ( token / quoted-string ) ]
 *
 * // valid parameter examples
 * "Link: <https://example.org>; rel; param1;"
 * "Link: <https://example.org>; rel="self"; param1="";"
 * }
 * </pre>
 * <p>
 * It is important that different parameter serialisation cases are handled correctly.
 * <p>
 * The following example shows three distinct cases that must be preserved during de-serialisation:
 *
 * <pre>
 * {@code
 * x=""  // empty double-quoted string
 * x="y" // double-quoted with content
 * x=y   // token value
 * x     // parameter name only
 * }
 * </pre>
 * <p>
 * These are all valid parameter serialisations.
 *
 *
 */
public record WebLinkParameter(String name, String value) {

  /**
   * Creates a new web link parameter with the provided name and value.
   *
   * @param name  the name of the web link parameter
   * @param value the value of the web link parameter
   */
  public static WebLinkParameter create(String name, String value) {
    return new WebLinkParameter(name, value);
  }

  /**
   * Creates a new web link parameter without a value.
   *
   * @param name the name of the parameter
   */
  public static WebLinkParameter withoutValue(String name) {
    return new WebLinkParameter(name, null);
  }

  /**
   * Checks if the web link parameter has a value.
   * <p>
   * The method will return {@code true} only when a value (including an empty one) has been
   * provided.
   *
   * @return {@code true}, if the parameter has a value (including an empty one). Returns
   * {@code false}, if no value has been provided
   */
  public boolean hasValue() {
    return value != null;
  }
}
