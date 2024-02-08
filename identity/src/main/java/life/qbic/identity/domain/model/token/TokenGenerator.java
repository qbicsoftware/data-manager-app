package life.qbic.identity.domain.model.token;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class TokenGenerator {

  private static final int TOKEN_LENGTH = 32;

  public TokenGenerator() {
  }

  private static Stream<Character> getRandomAlphabet() {
    Random random = new SecureRandom();
    List<Character> randomLetters = new java.util.ArrayList<>(
        Stream.concat(
                random.ints(TokenGenerator.TOKEN_LENGTH, 65, 91).mapToObj(data -> (char) data),
                random.ints(TokenGenerator.TOKEN_LENGTH, 97, 123).mapToObj(data -> (char) data))
            .toList());
    Collections.shuffle(randomLetters);
    return randomLetters.subList(0, TokenGenerator.TOKEN_LENGTH).stream();
  }

  private static Stream<Character> getRandomNumber() {
    Random random = new SecureRandom();
    return random.ints(TokenGenerator.TOKEN_LENGTH, 48, 58).mapToObj(data -> (char) data);
  }

  /**
   * Generates a pseudo-random character sequence
   *
   * @since 1.0.0
   */
  public String token() {
    var randomSequence = new ArrayList<>(
        Stream.concat(getRandomAlphabet(), getRandomNumber()).toList());
    Collections.shuffle(randomSequence);
    return randomSequence.subList(0, TOKEN_LENGTH).stream()
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
  }

}
