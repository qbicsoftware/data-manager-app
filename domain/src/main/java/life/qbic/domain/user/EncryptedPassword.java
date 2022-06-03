package life.qbic.domain.user;

import life.qbic.domain.user.User.UserException;
import life.qbic.domain.usermanagement.policies.PasswordEncryptionPolicy;
import life.qbic.domain.usermanagement.policies.PasswordPolicy;
import life.qbic.domain.usermanagement.policies.PolicyCheckReport;
import life.qbic.domain.usermanagement.policies.PolicyStatus;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
    return EncryptedPassword.fromEncrypted(encryptedPassword);
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

  public String hash() {
    return this.encryptedPassword;
  }

  public static class PasswordValidationException extends RuntimeException {

    PasswordValidationException() {
      super();
    }

    PasswordValidationException(String message) {
      super(message);
    }

  }

}
