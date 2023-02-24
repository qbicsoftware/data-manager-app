package life.qbic.projectmanagement.domain.project;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import javax.persistence.AttributeConverter;

/**
 * QBiC Project Code
 * <p>
 * Represents a project code, that is usually communicated with customers and internally to
 * reference a project.
 *
 * @since 1.0.0
 */
public class ProjectCode {

  public static final String[] BLACKLIST = new String[]{"FUCK", "SHIT"};

  private final String value;

  private static final int LENGTH = 5;

  private static final String PREFIX = "Q";

  public static final char[] ALLOWED_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWX".toCharArray();

  public static final char[] ALLOWED_NUMBERS = "0123456789".toCharArray();

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
    // project codes are always upper case
    str = str.toUpperCase();
    if (!(str.startsWith(PREFIX) && (str.length() == LENGTH))) {
      throw new IllegalArgumentException(String.format("%s is not a valid project code", str));
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

  private static boolean isValid(String code) {
    return code.startsWith(PREFIX) && (code.length() == LENGTH) && !containsInvalidCharacters(code);
  }

  private static boolean containsInvalidCharacters(String code) {
    return !containsValidCharacters(code);
  }

  private static boolean containsValidCharacters(String code) {
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
      return letters[new Random().nextInt(letters.length)];
    }

    private char randomNumber() {
      return numbers[new Random().nextInt(numbers.length)];
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

  public static class Converter implements AttributeConverter<ProjectCode, String> {

    @Override
    public String convertToDatabaseColumn(ProjectCode projectCode) {
      return projectCode.value();
    }

    @Override
    public ProjectCode convertToEntityAttribute(String s) {
      return ProjectCode.parse(s);
    }
  }
}
