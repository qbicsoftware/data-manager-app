package life.qbic.projectmanagement.domain.model.project;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

/**
 * QBiC Project Code
 * <p>
 * Represents a project code, that is usually communicated with customers and internally to
 * reference a project.
 *
 * @since 1.0.0
 */
@Embeddable
public class ProjectCode {

  private static final String[] BLACKLIST = new String[]{"FUCK", "SHIT"};

  @Column(name = "projectCode")
  private String value;

  private static final int LENGTH_RANDOM_PART = 4;

  private static final String PREFIX = "Q2";

  private static final char[] ALLOWED_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWX".toCharArray();

  private static final char[] ALLOWED_NUMBERS = "0123456789".toCharArray();

  private static final Random RANDOM = new SecureRandom();

  protected ProjectCode() {
    // Needed for JPA
  }

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
    var randomCodeGenerator = new RandomCodeGenerator(ALLOWED_LETTERS, ALLOWED_NUMBERS);
    String code = randomCodeGenerator.next(LENGTH_RANDOM_PART);
    while (isBlackListed(code)) {
      code = randomCodeGenerator.next(LENGTH_RANDOM_PART);
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
    // project codes are always upper case
    str = str.toUpperCase();
    if (!isGeneralFormatValid(str)) {
      throw new IllegalArgumentException(String.format("%s is not a valid project ID", str));
    }
    if (containsInvalidCharacters(str)) {
      throw new IllegalArgumentException(String.format("%s contains invalid characters", str));
    }
    if (isBlackListed(str.substring(1))) {
      throw new IllegalArgumentException(
          String.format("%s contains a blacklisted expression", str));
    }
    return new ProjectCode(str);
  }

  private static boolean isGeneralFormatValid(String code) {
    return code.startsWith(PREFIX) && (code.length() == LENGTH_RANDOM_PART + PREFIX.length());
  }

  private static boolean containsInvalidCharacters(String code) {
    return !containsOnlyValidCharacters(code);
  }

  private static boolean containsOnlyValidCharacters(String code) {
    char[] codeArray = code.toCharArray();
    for (int character = 0; character < code.length(); character++) {
      char currentCharacter = codeArray[character];
      if (isInvalidCharacter(currentCharacter)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isInvalidCharacter(char c) {
    if (isInvalidLetter(c)) {
      return isInvalidNumber(c);
    }
    return false;

  }

  private static boolean isInvalidNumber(char c) {
    return !isValidNumber(c);
  }

  private static boolean isValidNumber(char c) {
    for (char allowedNumber : ALLOWED_NUMBERS) {
      if (c == allowedNumber) {
        return true;
      }
    }
    return false;
  }

  private static boolean isInvalidLetter(char c) {
    return !isValidLetter(c);
  }

  private static boolean isValidLetter(char c) {
    for (char allowedLetter : ALLOWED_LETTERS) {
      if (c == allowedLetter) {
        return true;
      }
    }
    return false;
  }

  private static boolean isBlackListed(String word) {
    return Arrays.asList(BLACKLIST).contains(word);
  }

  private ProjectCode(String value) {
    this.value = value;
  }

  public static boolean isValid(String value) {
    boolean isValid;
    try {
      parse(value);
      isValid = true;
    } catch (IllegalArgumentException e) {
      isValid = false;
    }
    return isValid;
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

  @Override
  public String toString() {
    return "ProjectCode{" +
        "value='" + value + '\'' +
        '}';
  }

  public static int getLENGTH() {
    return LENGTH_RANDOM_PART + PREFIX.length();
  }

  public static String getPREFIX() {
    return PREFIX;
  }

  private static class RandomCodeGenerator {

    final char[] letters;

    final char[] numbers;

    public RandomCodeGenerator(char[] letterAlphabet, char[] numberAlphabet) {
      this.letters = letterAlphabet;
      this.numbers = numberAlphabet;
    }

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
      return letters[RANDOM.nextInt(letters.length)];
    }

    private char randomNumber() {
      return numbers[RANDOM.nextInt(numbers.length)];
    }

  }

  private static class Coin {

    enum SIDE {
      HEAD, TAILS
    }

    SIDE flip() {
      double randomValue = RANDOM.nextDouble(1);
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
