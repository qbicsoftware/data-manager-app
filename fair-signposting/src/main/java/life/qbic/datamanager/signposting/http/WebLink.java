package life.qbic.datamanager.signposting.http;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.datamanager.signposting.http.validation.RfcLinkParameter;

/**
 * A Java record representing a web link object following the
 * <a href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a> model specification.
 */
public record WebLink(URI reference, List<WebLinkParameter> params) {

  /**
   * Creates an <a href="https://datatracker.ietf.org/doc/html/rfc8288">RFC 8288</a> compliant web
   * link object.
   * <p>
   * Following RFC8288, the ABNF for a link parameter is:
   * <p>
   * {@code link-param = token BWS [ "=" BWS ( token / quoted-string ) ]}
   * <p>
   * The parameter key must not be withoutValue, so during construction the {@code params} keys are checked
   * for an withoutValue key. The values can be withoutValue though.
   *
   * @param reference a {@link URI} pointing to the actual resource
   * @param params    a {@link Map} of parameters as keys and a list of their values
   * @return the new Weblink
   * @throws FormatException      if the parameters violate any known specification described in the
   *                              RFC
   * @throws NullPointerException if any method argument is {@code null}
   */
  public static WebLink create(URI reference, List<WebLinkParameter> params)
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


  public Optional<String> anchor() {
    return Optional.empty();
  }

  public List<String> hreflang() {
    return List.of();
  }

  public Optional<String> media() {
    return Optional.empty();
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
  public List<String> rel() {
    return this.params.stream()
        .filter(param -> param.name().equals("rel"))
        .map(WebLinkParameter::value)
        .map(value -> value.split("\\s+"))
        .flatMap(Arrays::stream)
        .toList();
  }

  /**
   * Returns all "rev" parameter values of the link.
   * <p>
   * RFC 8288 section 3.3 does not specify the multiplicity of occurrence. But given the close
   * relation to the "rel" parameter and its definition in the same section, web link will treat the
   * "rev" parameter equally.
   * <p>
   * As with the "rel" parameter, multiple regular relation types are allowed when they are
   * separated by one or more space characters (SP = ASCII 0x20):
   * <p>
   * {@code relation-type *( 1*SP relation-type ) }.
   * <p>
   * The method returns space-separated values as individual values of the "rel" parameter.
   *
   * @return a list of relation parameter values
   */
  public List<String> rev() {
    return this.params.stream()
        .filter(param -> param.name().equals("rev"))
        .map(WebLinkParameter::value)
        .map(value -> value.split("\\s+"))
        .flatMap(Arrays::stream)
        .toList();
  }

  public Optional<String> title() {
    return Optional.empty();
  }

  public Optional<String> titleMultiple() {
    return Optional.empty();
  }

  public Optional<String> type() {
    return Optional.empty();
  }

  public Map<String, List<String>> extensionAttributes() {
    Set<String> rfcParameterNames = Arrays.stream(RfcLinkParameter.values())
        .map(RfcLinkParameter::rfcValue)
        .collect(Collectors.toSet());
    return this.params.stream()
        .filter(param -> !rfcParameterNames.contains(param.name()))
        .collect(Collectors.groupingBy(WebLinkParameter::name,
            Collectors.mapping(WebLinkParameter::value, Collectors.toList())));
  }

  public List<String> extensionAttribute(String name) {
    return extensionAttributes().getOrDefault(name, List.of());
  }
}
