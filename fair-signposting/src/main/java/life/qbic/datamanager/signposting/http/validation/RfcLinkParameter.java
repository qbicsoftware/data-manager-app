package life.qbic.datamanager.signposting.http.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Standard parameters for the {@code Link} HTTP header.
 *
 * <ul>
 *   <li>"anchor" - see RFC 8288 section 3.2 (“Link Context”)</li>
 *   <li>"hreflang" - see RFC 8288 section 3.4.1 (“The hreflang Target Attribute”)</li>
 *   <li>"media" - see RFC 8288 section 3.4.2 (“The media Target Attribute”)</li>
 *   <li>"rel" - see RFC 8288 section 3.3 (“Relation Types”)</li>
 *   <li>"rev" - see RFC 8288 section 3.3 (historical note)</li>
 *   <li>"title" - see RFC 8288 section 3.4.4 (“The title Target Attribute”)</li>
 *   <li>"title*" - see RFC 8288 section 3.4.4 references RFC 5987 (“Character Set and Language Encoding for HTTP Header Field Parameters”)</li>
 *   <li>"type" - see RFC 8288 section 3.4.3 (“The type Target Attribute”)</li>
 * </ul>
 */
public enum RfcLinkParameter {

  ANCHOR("anchor"),
  HREFLANG("hreflang"),
  MEDIA("media"),
  REL("rel"),
  REV("rev"),
  TITLE("title"),
  TITLE_MULT("title*"),
  TYPE("type");

  private final String value;

  private static final Map<String, RfcLinkParameter> LOOKUP = new HashMap<>();

  static {
    for (RfcLinkParameter p : RfcLinkParameter.values()) {
      LOOKUP.put(p.value, p);
    }
  }

  RfcLinkParameter(String value) {
    this.value = value;
  }

  /**
   * Returns the RFC compliant value of the parameter name.
   *
   * @return the alpha-value of the link parameter
   */
  public String rfcValue() {
    return value;
  }

  /**
   * Creates an RfcLinkParameter from a given value, if the value belongs to any existing enum of
   * this type.
   *
   * @param value the value to match the corresponding enum value
   * @return the corresponding enum in an Optional, of returns Optional.withoutValue()
   */
  public static Optional<RfcLinkParameter> from(String value) {
    return Optional.ofNullable(LOOKUP.getOrDefault(value, null));
  }

}
