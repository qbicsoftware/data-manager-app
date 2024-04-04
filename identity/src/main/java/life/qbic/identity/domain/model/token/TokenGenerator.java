package life.qbic.identity.domain.model.token;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * <b>Token Generator</b>
 *
 * <p>Creates random token strings width a default length of 32 characters, using a combination
 * Java's {@link SecureRandom} class and the {@link Collections#shuffle(List)} method.</p>
 * <p>
 * The token character pool are small and capital letters from the ASCII table ans well as numeric
 * values from 0-9.
 *
 * @since 1.0.0
 */
public class TokenGenerator {

  private static final int TOKEN_LENGTH = 32;

  private TokenGenerator() {
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
  public static String token() {
    var randomSequence = new ArrayList<>(
        Stream.concat(getRandomAlphabet(), getRandomNumber()).toList());
    Collections.shuffle(randomSequence);
    return randomSequence.subList(0, TOKEN_LENGTH).stream()
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
  }

}
