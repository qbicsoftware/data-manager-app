package life.qbic.authentication.application.security;

import life.qbic.authentication.domain.user.policy.PasswordEncryptionPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <b>QBiC's implementation of the PasswordEncoder interface</b>
 *
 * <p>This class shall be used to encode and match passwords in the context of user authentication.
 *
 * @since 1.0.0
 */
public class QBiCPasswordEncoder implements PasswordEncoder {

  private final PasswordEncryptionPolicy passwordEncryptionPolicy;

  public QBiCPasswordEncoder() {
    passwordEncryptionPolicy = PasswordEncryptionPolicy.instance();
  }

  /**
   * Takes a raw password and encodes it based on our business rules.
   *
   * <p>Note: The original raw password gets overwritten after the encoding is finished.
   *
   * @param rawPassword the password to be encoded
   * @return the encoded password
   */
  @Override
  public String encode(CharSequence rawPassword) {
    return passwordEncryptionPolicy.encrypt(rawPassword.toString().toCharArray());
  }

  /**
   * Matches a given raw password with an encoded password and checks if they are the same
   *
   * @param rawPassword     the raw password to be encoded from the user
   * @param encodedPassword the encoded password from the database
   * @return true, if the passwords match
   */
  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return passwordEncryptionPolicy.doPasswordsMatch(
        rawPassword.toString().toCharArray(), encodedPassword);
  }
}
