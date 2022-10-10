package life.qbic.projectmanagement.domain.project;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectCode {

  static final String[] BLACKLIST = new String[]{"FUCK", "SHIT"};

  private final String value;

  private static final int LENGTH = 4;

  private static final String PREFIX = "Q";

  public static ProjectCode random() {
    var randomCodeGenerator = new RandomCodeGenerator();
    String code = randomCodeGenerator.next(LENGTH);
    while (isBlackListed(code)) {
      code = randomCodeGenerator.next(LENGTH);
    }
    return new ProjectCode(PREFIX + code);
  }

  private static boolean isBlackListed(String word) {
    return Arrays.stream(BLACKLIST).anyMatch(word::equals);
  }

  private ProjectCode(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }

  static class RandomCodeGenerator {


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

    public SIDE flip() {
      double randomValue = new Random().nextDouble(1);
      if (randomValue < 0.5) {
        return SIDE.HEAD;
      } else {
        return SIDE.TAILS;
      }
    }

    public Supplier<Character> flip(Supplier<Character> supplierIfHead,
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
