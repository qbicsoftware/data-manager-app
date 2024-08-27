package life.qbic.datamanager.parser;

import java.util.Objects;

/**
 * <b>Sanitizer</b>
 *
 * <p>Cleans String literals according to encoding requirements for parsing.</p>
 *
 * @since 1.4.0
 */
public class Sanitizer {

  private static final String ASTERIX = "\\*";

  /**
   * Removes all available `*` (asterix) symbols, executes {@link String#trim()} and
   * {@link String#toLowerCase()} on a given input String.
   *
   * @param value the String value to be sanitized
   * @return the sanitized value
   * @since 1.4.0
   */
  public static String headerEncoder(String value) {
    Objects.requireNonNull(value);
    return value.replaceAll(ASTERIX, "").trim().toLowerCase();
  }

}
