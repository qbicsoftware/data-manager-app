package life.qbic.logging.subscription.provider.mail.property;

/**
 * <b>Placeholder</b>
 * <p>
 * Represents a value of a property placeholder.
 * <p>
 * The syntax for placeholders is like the following:
 * <p>
 * <code>property-name=${MY_ENV_VAR}</code>
 * <p>
 *
 * @since 1.0.0
 */
public class Placeholder {

  private static final String PLACEHOLDER_START_CHARS_REGEX = "\\$\\{";

  private static final String PLACEHOLDER_END_CHARS_REGEX = "}";
  private final String placeholder;

  private Placeholder(String placeholder) {
    this.placeholder = placeholder;
  }

  /**
   * Returns the placeholder name
   *
   * @return the name of the placeholder
   * @since 1.0.0
   */
  public String name() {
    return this.placeholder;
  }

  /**
   * Creates an instance of a {@link Placeholder} object.
   *
   * @param str A string value that is a potential placeholder
   * @return the placeholder containing the name of the placeholder variable
   * @throws IllegalArgumentException when the passed parameter {@link String} value is not
   *                                  recognized as a valid placeholder following the specified
   *                                  syntax
   * @since 1.0.0
   */
  static Placeholder create(String str) throws IllegalArgumentException {
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
