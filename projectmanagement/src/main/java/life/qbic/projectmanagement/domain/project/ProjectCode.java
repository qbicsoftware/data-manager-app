package life.qbic.projectmanagement.domain.project;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

/**
 * QBiC Project Code
 *
 * Represents a project code, that is usually communicated with customers and internally
 * to reference a project.
 *
 * @since 1.0.0
 */
public class ProjectCode {

  public static final String[] BLACKLIST = new String[]{"FUCK", "SHIT"};

  private final String value;

  private static final int LENGTH = 5;

  private static final String PREFIX = "Q";

  /**
   * Creates a random project code containing of letters from the English alphabet and natural
   * numbers.
   * <p>
   * A project code always starts with the prefix 'Q' and has a total length of 5.
   *
   * @return a random project code
   * @since 1.0.0
   */
  public static ProjectCode random() {
    var randomCodeGenerator = new RandomCodeGenerator();
    String code = randomCodeGenerator.next(LENGTH - 1);
    while (isBlackListed(code)) {
      code = randomCodeGenerator.next(LENGTH - 1);
    }
    return new ProjectCode(PREFIX + code);
  }


  /**
   * Parses a putative project code in String representation into its object representation.
   *
   * @param str the putative project code
   * @return the code in its object representation
   * @throws IllegalArgumentException if the argument is not a valid project code, for example when
   *                                  it does not have the correct length (5), does not start with
   *                                  the prefix 'Q' or contains a blacklisted expression
   * @since 1.0.0
   */
  public static ProjectCode parse(String str) throws IllegalArgumentException {
    if (isInvalid(str)) {
      throw new IllegalArgumentException(String.format("%s is not a valid project code", str));
    }
    if (isBlackListed(str.substring(1))) {
      throw new IllegalArgumentException(
          String.format("%s contains a blacklisted expression", str));
    }
    return new ProjectCode(str);
  }

  private static boolean isValid(String code) {
    return code.startsWith(PREFIX) && (code.length() == LENGTH);
  }

  private static boolean isInvalid(String code) {
    return !isValid(code);
  }

  private static boolean isBlackListed(String word) {
    return Arrays.asList(BLACKLIST).contains(word);
  }

  private ProjectCode(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectCode that = (ProjectCode) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  private static class RandomCodeGenerator {

    static final char[] LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    static final char[] NUMBERS = "0123456789".toCharArray();

    public String next(int length) {
      Coin coin = new Coin();
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < length; i++) {
        builder.append(generateRandomChar(coin));
      }
      return builder.toString();
    }

    private char generateRandomChar(Coin coin) {
      return coin.flip(this::randomNumber, this::randomLetter).get();
    }

    private char randomLetter() {
      return LETTERS[new Random().nextInt(LETTERS.length)];
    }

    private char randomNumber() {
      return NUMBERS[new Random().nextInt(NUMBERS.length)];
    }

  }

  private static class Coin {

    enum SIDE {
      HEAD, TAILS
    }

    SIDE flip() {
      double randomValue = new Random().nextDouble(1);
      if (randomValue < 0.5) {
        return SIDE.HEAD;
      } else {
        return SIDE.TAILS;
      }
    }

    Supplier<Character> flip(Supplier<Character> supplierIfHead,
        Supplier<Character> supplierIfTails) {
      var side = flip();
      if (side.equals(SIDE.HEAD)) {
        return supplierIfHead;
      } else {
        return supplierIfTails;
      }
    }
  }
}
