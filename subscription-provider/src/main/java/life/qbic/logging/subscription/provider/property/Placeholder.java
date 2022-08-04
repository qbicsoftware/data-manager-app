package life.qbic.logging.subscription.provider.property;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Placeholder {

  private static final String PLACEHOLDER_START_CHARS_REGEX = "\\$\\{";

  private static final String PLACEHOLDER_END_CHARS_REGEX = "}";

  public static boolean isPlaceholder(String str) {
    return !parsePlaceholder(str).isBlank();
  }

  public static String placeholderName(String str) {
    return parsePlaceholder(str);
  }

  private static String parsePlaceholder(String str) {
    var placeholder = new StringBuilder();
    var start = placeholderStart(str);
    var end = placeholderEnd(str);
    if (start == -1 || end == -1) {
      return "";
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
