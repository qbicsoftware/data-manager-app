package life.qbic.security;

import life.qbic.usermanagement.policies.PasswordEncryptionPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class QBiCPasswordEncoder implements PasswordEncoder {

  private final PasswordEncryptionPolicy passwordEncryptionPolicy;

  public QBiCPasswordEncoder() {
    passwordEncryptionPolicy = PasswordEncryptionPolicy.create();
  }

  @Override
  public String encode(CharSequence rawPassword) {
    return passwordEncryptionPolicy.encrypt(rawPassword.toString());
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return passwordEncryptionPolicy.comparePassword(rawPassword.toString().toCharArray(), encodedPassword);
  }
}
