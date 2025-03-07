package life.qbic.identity.domain.model.token;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import static java.util.Objects.requireNonNull;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import life.qbic.logging.api.Logger;
import static life.qbic.logging.service.LoggerFactory.logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Encodes personal access tokens
 */
@Service
public class PersonalAccessTokenEncoder implements TokenEncoder {

  private static final Logger log = logger(PersonalAccessTokenEncoder.class);

  private final byte[] salt;
  private final int iterationCount;

  private static final int EXPECTED_MIN_ITERATION_COUNT = 100_000;
  private static final int EXPECTED_MIN_SALT_BITS = 128;
  public static final int EXPECTED_MIN_SALT_BYTES = (int) Math.ceil(
      (double) EXPECTED_MIN_SALT_BITS / 8);

  private static final int ITERATION_COUNT_INDEX = 0; // the index of the iteration count in the encoded token
  private static final int SALT_INDEX = 1; // the index of the salt content in the encoded token
  private static final int HASH_INDEX = 2; // the index of the hash content in the encoded token

  public PersonalAccessTokenEncoder(
      @Value("${qbic.access-token.salt}") String salt,
      @Value("${qbic.access-token.iteration-count}") int iterationCount) {
    this.salt = fromHex(requireNonNull(salt, "salt must not be null"));
    if (this.salt.length < EXPECTED_MIN_SALT_BYTES) {
      throw new IllegalArgumentException(
          "salt must have at least " + EXPECTED_MIN_SALT_BITS + " bits. Provided: "
              + this.salt.length);
    }
    if (iterationCount < EXPECTED_MIN_ITERATION_COUNT) {
      throw new IllegalArgumentException(
          "Iteration count n=" + iterationCount + " cannot be less than n="
              + EXPECTED_MIN_ITERATION_COUNT);
    }
    this.iterationCount = iterationCount;
  }

  private record EncryptionSettings(String cipher, int keyBitSize) {

  }

  private static final EncryptionSettings ENCRYPTION_SETTINGS = new EncryptionSettings("AES", 256);


  @Override
  public String encode(char[] token) {
    var iterationCountCopy = this.iterationCount;
    byte[] hash = pbe(token, this.salt, iterationCountCopy);
    return iterationCountCopy + ":" + toHex(this.salt) + ":" + toHex(hash);
  }

  @Override
  public boolean matches(char[] token, String encodedToken) {
    byte[] readSalt = readSalt(encodedToken);
    if (!Arrays.equals(readSalt, this.salt)) {
      log.warn("Personal access token has different salt than currently configured.");
    }
    int readIterationCount = readIterationCount(encodedToken);
    if (readIterationCount != this.iterationCount) {
      log.warn("Personal access token has different iterations than currently configured.");
    }
    byte[] asEncoded = pbe(token, readSalt, readIterationCount);
    return compareSecure(asEncoded, readHash(encodedToken));
  }


  /**
   * Slow comparison method, making it impossible for timing attacks to reverse-engineer the hash.
   *
   * @param a one byte array
   * @param b another byte array to compare
   * @return true, if both byte arrays contents are equal, else false
   * @since 1.0.0
   */
  private static boolean compareSecure(byte[] a, byte[] b) {
    int difference = a.length ^ b.length;
    for (int i = 0; i < a.length && i < b.length; i++) {
      difference |= a[i] ^ b[i];
    }
    return difference == 0;
  }

  private static byte[] readSalt(String encodedToken) {
    return fromHex(encodedToken.split(":")[SALT_INDEX]);
  }

  private static int readIterationCount(String encodedToken) {
    return Integer.parseInt(encodedToken.split(":")[ITERATION_COUNT_INDEX]);
  }

  private static byte[] readHash(String encodedToken) {
    return fromHex(encodedToken.split(":")[HASH_INDEX]);
  }

  /**
   * Converts a byte array into a hexadecimal String representation.
   *
   * @param bytes a byte array
   * @return the hexadecimal String representation
   * @since 1.0.0
   */
  private static String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder();
    for (byte abyte : bytes) {
      builder.append(String.format("%02x", abyte));
    }
    return builder.toString();
  }

  /**
   * Converts a hexadecimal String representation to a byte array
   *
   * @param hex the hexadecimal String
   * @return the converted byte array
   * @since 1.0.0
   */
  private static byte[] fromHex(String hex) {
    byte[] binary = new byte[hex.length() / 2];
    for (int i = 0; i < binary.length; i++) {
      var startIdxInclusive = i * 2;
      var endIdxInclusive =
          startIdxInclusive + 1 >= hex.length() ? startIdxInclusive : startIdxInclusive + 1;
      binary[i] = (byte) Integer.parseInt(
          hex.substring(startIdxInclusive, endIdxInclusive + 1), 16);
    }
    return binary;
  }

  /**
   * Uses Password-Based Encryption to encrypt a token given a salt and iterations
   *
   * @param token          the token to be encrypted
   * @param salt           the salt used in the encryption
   * @param iterationCount the number of iterations
   * @return encryption result
   */
  private static byte[] pbe(char[] token, byte[] salt, int iterationCount) {
    KeySpec spec =
        new PBEKeySpec(token, salt, iterationCount, ENCRYPTION_SETTINGS.keyBitSize());
    SecretKey secretKey;
    try {
      SecretKeyFactory result;
      result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      SecretKeyFactory factory = result;
      secretKey = factory.generateSecret(spec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException("error encrypting token: " + e.getMessage());
    }
    return new SecretKeySpec(secretKey.getEncoded(), ENCRYPTION_SETTINGS.cipher()).getEncoded();
  }

}
