package life.qbic.datamanager.parser;

import java.util.Arrays;
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
    return value.replaceAll(ASTERIX, "").trim();
  }

  /**
   * Investigates an array for information and can be used to e.g. filter out blank arrays.
   * <p>
   * An array is considered to contain information, if at least one element contains a value that is
   * NOT {@link String#isEmpty()} and NOT {@link String#isBlank()}.
   * <p>
   * If the array contains only empty or blank values, the function returns <code>false</code>.
   *
   * @param array the array to investigate
   * @return <code>true</code>, if at least one value is not blank or empty, else returns
   * <code>false</code>
   * @since 1.4.0
   */
  public static boolean containsInformation(String[] array) {
    return !Arrays.stream(array).allMatch(value -> value.isEmpty() || value.isBlank());
  }

}
