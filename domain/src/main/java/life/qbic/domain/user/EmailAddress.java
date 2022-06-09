package life.qbic.domain.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.apps.datamanager.ApplicationException;
import life.qbic.domain.usermanagement.policies.EmailFormatPolicy;
import life.qbic.domain.usermanagement.policies.PolicyCheckReport;
import life.qbic.domain.usermanagement.policies.PolicyStatus;

/**
 * <b>EmailAddress</b>
 * <p>
 * Represents a valid email address, specified by RFC5322.
 * <p>
 * For mor details please check {@link EmailFormatPolicy}.
 *
 * @since 1.0.0
 */
public class EmailAddress implements Serializable {

  private String address;

  /**
   * Creates an email object instance from a String representation of an email address.
   * <p>
   * This method performs a format validation.
   *
   * @param s the email address String
   * @return an email
   * @throws EmailValidationException if the email format is not a valid email format specified by
   *                                  RFC5322
   * @since 1.0.0
   */
  public static EmailAddress from(String s) throws EmailValidationException {
    PolicyCheckReport policyCheckReport = EmailFormatPolicy.instance().validate(s);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new EmailValidationException(policyCheckReport, s);
    }
    var email = new EmailAddress();
    email.set(s);
    return email;
  }

  private EmailAddress() {
    super();
  }

  private void set(String s) {
    address = s;
  }

  /**
   * Queries the email address as String representation.
   *
   * @return email address as String
   * @since 1.0.0
   */
  public String get() {
    return this.address;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailAddress emailAddress = (EmailAddress) o;
    return address.equals(emailAddress.get());
  }

  @Override
  public int hashCode() {
    return Objects.hash(address);
  }


  /**
   * <h1>Exception that indicates violations during the email address validation process/h1>
   *
   * <p>This exception is supposed to be thrown, if the provided email address for an user violates
   * the format specified by
   * RFC5322 It's intention is to contain the invalid email address and the PolicyCheckReport for
   * the violated policy</p>
   *
   * @since 1.0.0
   */
  public static class EmailValidationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -4253849498611530692L;

    private final String invalidEmailAddress;

    private final transient PolicyCheckReport emailPolicyCheckReport;

    EmailValidationException(PolicyCheckReport emailAddressCheckReport,
        String invalidEmailAddress) {
      super();
      this.emailPolicyCheckReport = emailAddressCheckReport;
      this.invalidEmailAddress = invalidEmailAddress;
    }

    public String getInvalidEmailAddress() {
      return invalidEmailAddress;
    }

    public PolicyCheckReport getEmailPolicyCheckReport() {
      return emailPolicyCheckReport;
    }
  }
}
