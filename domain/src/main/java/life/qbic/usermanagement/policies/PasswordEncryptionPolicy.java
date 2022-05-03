package life.qbic.usermanagement.policies;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * <b>Password encryption policy</b>
 * <p>
 * Holds the current business policy for password encryption and provides functionality to execute
 * the policy on clear text passwords.
 *
 * @since 1.0.0
 */
public class PasswordEncryptionPolicy {

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
   * Encrypts a password using Java's PBKDF2 implementation.
   * <p>
   *
   * @param password the password to encrypt
   * @return the encrypted password
   * @since 1.0.0
   */
  public String encrypt(String password) {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
    SecretKeyFactory factory = tryToGetSecretKeyFactory();
    return new String(tryToGetSecretKey(factory, spec).getEncoded());
  }

  private SecretKeyFactory tryToGetSecretKeyFactory() {
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    } catch (NoSuchAlgorithmException ignored) {
      throw new RuntimeException("Unexpected exception, encryption failed.");
    }
  }

  private SecretKey tryToGetSecretKey(SecretKeyFactory factory, KeySpec keySpec) {
    try {
      return factory.generateSecret(keySpec);
    } catch (InvalidKeySpecException ignored) {
      throw new RuntimeException("Failed to generate secret key for password hashing.");
    }
  }
}
