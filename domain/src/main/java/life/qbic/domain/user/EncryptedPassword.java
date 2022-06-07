package life.qbic.domain.user;

import java.io.Serial;
import java.util.Objects;
import life.qbic.apps.datamanager.ApplicationException;
import life.qbic.domain.usermanagement.policies.PasswordEncryptionPolicy;
import life.qbic.domain.usermanagement.policies.PasswordPolicy;
import life.qbic.domain.usermanagement.policies.PolicyCheckReport;
import life.qbic.domain.usermanagement.policies.PolicyStatus;

/**
 * <b>Encrypted Password</b>
 * <p>
 * Represents an encrypted user password.
 *
 * @since 1.0.0
 */
public class EncryptedPassword {

  private String encryptedPassword;

  /**
   * Sets a password for the current user.
   *
   * <p>Beware that the password gets validated against the current password policy. If the
   * password violates the policy, an {@link PasswordValidationException} is thrown.
   *
   * <p>The password is then stored in an encrypted form, controlled by the {@link
   * PasswordEncryptionPolicy}.
   *
   * <p>It is the client's responsibility to reset the raw password, after it has been set for the
   * user and encrypted.
   *
   * @param rawPassword the new user password
   * @throws PasswordValidationException if the user password is too weak
   * @since 1.0.0
   */
  public static EncryptedPassword from(char[] rawPassword) throws PasswordValidationException {
    PolicyCheckReport policyCheckReport = PasswordPolicy.create().validate(rawPassword);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new PasswordValidationException(policyCheckReport.reason());
    }
    var password = new EncryptedPassword();
    password.setEncryptedPassword(encryptPassword(rawPassword));
    return password;
  }

  protected static EncryptedPassword fromEncrypted(String encryptedPassword) {
    return new EncryptedPassword(encryptedPassword);
  }

  protected EncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  private EncryptedPassword() {
    super();
  }

  private void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  private static String encryptPassword(char[] rawPassword) {
    return PasswordEncryptionPolicy.create().encrypt(rawPassword);
  }

  /**
   * Returns the passwords encrypted hash value
   *
   * @return
   * @since 1.0.0
   */
  public String hash() {
    return this.encryptedPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EncryptedPassword that = (EncryptedPassword) o;
    return Objects.equals(encryptedPassword, that.encryptedPassword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(encryptedPassword);
  }

  public static class PasswordValidationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -3732749830794920567L;

    PasswordValidationException() {
      super();
    }

    PasswordValidationException(String message) {
      super(message);
    }

  }

}
