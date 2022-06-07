package life.qbic.domain.user;

import java.io.Serial;
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
public class EmailAddress {

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
    PolicyCheckReport policyCheckReport = EmailFormatPolicy.create().validate(s);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new EmailValidationException(policyCheckReport.reason());
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
  public String address() {
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
    return address.equals(emailAddress.address());
  }

  @Override
  public int hashCode() {
    return Objects.hash(address);
  }

  public static class EmailValidationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -4253849498611530692L;

    EmailValidationException() {
      super();
    }

    EmailValidationException(String message) {
      super(message);
    }

  }
}
