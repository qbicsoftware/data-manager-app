package life.qbic.usermanagement.policies;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * <b>Password encryption policy</b>
 *
 * <p>Holds the current business policy for password encryption and provides functionality to
 * execute the policy on clear text passwords.
 *
 * @since 1.0.0
 */
public class PasswordEncryptionPolicy {

  private static final int ITERATION_INDEX =
      0; // the index of the iteration count in the encoded password String
  private static final int SALT_INDEX =
      1; // the index of the salt content in the encoded password String
  private static final int HASH_INDEX =
      2; // the index of the hash content in the encoded password String
  private static final int ITERATIONS =
      10_000; // the iteration count used for the encryption algorithm
  private static final int KEY_BYTES = 20; // the key byte value for the encryption algorithm
  private static final int SALT_BYTES = 20; // the salt byte value for the salt generation
  private static PasswordEncryptionPolicy INSTANCE;

  /**
   * Creates a {@link PasswordEncryptionPolicy} reference. Following the singleton pattern an object
   * is only instanced once this method is called the first time.
   *
   * @return the password encryption policy
   * @since 1.0.0
   */
  public static PasswordEncryptionPolicy create() {
    if (INSTANCE == null) {
      INSTANCE = new PasswordEncryptionPolicy();
    }
    return INSTANCE;
  }

  /**
   * Encrypts a password using Java's PBE implementation.
   *
   * <p></br>
   *
   * @param password the cleartext password to encrypt
   * @return the encrypted password
   * @since 1.0.0
   */
  public String encrypt(String password) {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[SALT_BYTES];
    random.nextBytes(salt);
    byte[] hash = pbe(password.toCharArray(), salt, ITERATIONS);
    return ITERATIONS + ":" + toHex(salt) + ":" + toHex(hash);
  }

  private static byte[] pbe(char[] password, byte[] salt, int iterations) {
    KeySpec spec =
        new PBEKeySpec(password, salt, iterations, PasswordEncryptionPolicy.KEY_BYTES * 8);
    SecretKeyFactory factory = getSecretKeyFactory();
    return createSecretKey(factory, spec).getEncoded();
  }

  /**
   * Compares a provided raw password with an encrypted hash.
   *
   * @param rawPassword the raw password string to match
   * @param encryptedHash the hash to match against
   * @return true, if the raw password matches the hash, else false
   * @since 1.0.0
   */
  public boolean doPasswordsMatch(char[] rawPassword, String encryptedHash) {
    String[] passwordParameters = encryptedHash.split(":");
    int iterations = Integer.parseInt(passwordParameters[ITERATION_INDEX]);
    byte[] salt = fromHex(passwordParameters[SALT_INDEX]);
    byte[] hash = fromHex(passwordParameters[HASH_INDEX]);

    byte[] potentialPassword = pbe(rawPassword, salt, iterations);
    return validate(potentialPassword, hash);
  }

  /**
   * Slow comparison method, making it impossible for timing attacks to reverse-engineer the hash.
   *
   * @param a one byte array
   * @param b another byte array to compare
   * @return true, if both byte arrays contents are equal, else false
   * @since 1.0.0
   */
  private static boolean validate(byte[] a, byte[] b) {
    int difference = a.length ^ b.length;
    for (int i = 0; i < a.length && i < b.length; i++) {
      difference |= a[i] ^ b[i];
    }
    return difference == 0;
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
      binary[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
    }
    return binary;
  }

  private static SecretKeyFactory getSecretKeyFactory() {
    try {
      return SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
    } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new EncryptionException(
          "Unexpected exception, encryption failed.", noSuchAlgorithmException);
    }
  }

  private static SecretKey createSecretKey(SecretKeyFactory factory, KeySpec keySpec) {
    try {
      return factory.generateSecret(keySpec);
    } catch (InvalidKeySpecException invalidKeySpecException) {
      throw new EncryptionException(
          "Failed to generate secret key for password hashing.", invalidKeySpecException);
    }
  }

  static class EncryptionException extends RuntimeException {
    EncryptionException(String reason, Exception cause) {
      super(reason, cause);
    }
  }
}
