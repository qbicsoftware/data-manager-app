package life.qbic.domain.user;

import java.io.Serial;
import java.io.Serializable;
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
public class EncryptedPassword implements Serializable {

  private String value;

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
    PolicyCheckReport policyCheckReport = PasswordPolicy.instance().validate(rawPassword);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new PasswordValidationException(policyCheckReport);
    }
    var password = new EncryptedPassword();
    password.setEncryptedPassword(encryptPassword(rawPassword));
    return password;
  }

  protected static EncryptedPassword fromEncrypted(String encryptedPassword) {
    return new EncryptedPassword(encryptedPassword);
  }

  protected EncryptedPassword(String encryptedPassword) {
    this.value = encryptedPassword;
  }

  private EncryptedPassword() {
    super();
  }

  private void setEncryptedPassword(String encryptedPassword) {
    this.value = encryptedPassword;
  }

  private static String encryptPassword(char[] rawPassword) {
    return PasswordEncryptionPolicy.instance().encrypt(rawPassword);
  }

  /**
   * Returns the passwords encrypted value value
   *
   * @since 1.0.0
   */
  public String value() {
    return this.value;
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
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  /**
   * <h1>Exception that indicates violations during the password validation process/h1>
   *
   * <p>This exception is supposed to be thrown, if the provided password for an user violates
   * the specifications set by the PasswordPolicy. It's intention is to contain the
   * PolicyCheckReport for the violated policy</p>
   *
   * @since 1.0.0
   */
  public static class PasswordValidationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -3732749830794920567L;

    private final transient PolicyCheckReport passwordPolicyCheckReport;

    PasswordValidationException(PolicyCheckReport policyCheckReport) {
      super();
      this.passwordPolicyCheckReport = policyCheckReport;
    }

    public PolicyCheckReport getPasswordPolicyCheckReport() {
      return passwordPolicyCheckReport;
    }

  }

}
