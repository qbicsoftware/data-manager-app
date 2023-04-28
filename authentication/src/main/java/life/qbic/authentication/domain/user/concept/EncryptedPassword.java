package life.qbic.authentication.domain.user.concept;

import life.qbic.application.commons.ApplicationException;
import life.qbic.authentication.domain.policy.PolicyCheckReport;
import life.qbic.authentication.domain.policy.PolicyStatus;
import life.qbic.authentication.domain.user.policy.PasswordEncryptionPolicy;
import life.qbic.authentication.domain.user.policy.PasswordPolicy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * <b>Encrypted Password</b>
 * <p>
 * Represents an encrypted user password.
 *
 * @since 1.0.0
 */
public class EncryptedPassword implements Serializable {

  private final String value;

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
    return new EncryptedPassword(encryptPassword(rawPassword));
  }

  public static EncryptedPassword fromEncrypted(String encryptedPassword) {
    return new EncryptedPassword(encryptedPassword);
  }

  private EncryptedPassword(String encryptedPassword) {
    super();
    this.value = encryptedPassword;
  }


  private static String encryptPassword(char[] rawPassword) {
    return PasswordEncryptionPolicy.instance().encrypt(rawPassword);
  }

  /**
   * Returns the passwords encrypted value
   *
   * @since 1.0.0
   */
  public String get() {
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
