package life.qbic.usermanagement.policies;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
   * is only instanced once, when this method is called the first time.
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
   * Encrypts a password using a simple Base64 encoding.
   * <p>
   * //TODO implement real encryption
   *
   * @param password the password to encrypt
   * @return the encrypted password
   * @since 1.0.0
   */
  public String encrypt(String password) {
    //Todo implement a sophisticated encryption like AES with a 256-bit key
    return new String(Base64.getEncoder().encode(password.getBytes(StandardCharsets.UTF_8)));
  }

}
