package life.qbic.security;

import life.qbic.usermanagement.policies.PasswordEncryptionPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <b>QBiC's implementation of the PasswordEncoder interface</b>
 * <p>
 * This class shall be used to encode and match passwords in the context of user authentication.
 *
 * @since 1.0.0
 */
public class QBiCPasswordEncoder implements PasswordEncoder {

  private final PasswordEncryptionPolicy passwordEncryptionPolicy;

  public QBiCPasswordEncoder() {
    passwordEncryptionPolicy = PasswordEncryptionPolicy.create();
  }

  /**
   * Takes a raw password and encodes it based on our business rules.
   * <p>
   * Note: The original raw password gets overwritten after the encoding is finished.
   *
   * @param rawPassword the password to be encoded
   * @return the encoded password
   */
  @Override
  public String encode(CharSequence rawPassword) {
    return passwordEncryptionPolicy.encrypt(rawPassword.toString());
  }

  /**
   * @param rawPassword
   * @param encodedPassword
   * @return
   */
  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return passwordEncryptionPolicy.comparePassword(rawPassword.toString().toCharArray(),
        encodedPassword);
  }
}
