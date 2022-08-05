package life.qbic.logging.subscription.provider.email.property;

/**
 * <b>Placeholder</b>
 *
 * @since 1.0.0
 */
public class Placeholder {

  private static final String PLACEHOLDER_START_CHARS_REGEX = "\\$\\{";

  private static final String PLACEHOLDER_END_CHARS_REGEX = "}";
  private final String placeholder;

  public Placeholder(String placeholder) {
    this.placeholder = placeholder;
  }

  public String value() {
    return this.placeholder;
  }

  static Placeholder create(String str) {
    var placeholder = parsePlaceholder(str);
    if (placeholder.equals(str)) {
      throw new IllegalArgumentException(
          "Input string is not a known placeholder. Placeholder must start with '${' and end with '}'");
    }
    return new Placeholder(placeholder);
  }

  private static String parsePlaceholder(String str) {
    var placeholder = new StringBuilder();
    var start = placeholderStart(str);
    var end = placeholderEnd(str);
    if (start == -1 || end == -1) {
      return str;
    }
    placeholder.append(str, start, end);
    return extractPlaceholderName(placeholder.toString()).trim();
  }

  private static String extractPlaceholderName(String str) {
    return str.replaceAll(PLACEHOLDER_START_CHARS_REGEX, "")
        .replaceAll(PLACEHOLDER_END_CHARS_REGEX, "");
  }

  private static int placeholderStart(String str) {
    return str.indexOf("${");
  }

  private static int placeholderEnd(String str) {
    return str.indexOf("}");
  }

}
