package life.qbic.authentication.domain.user.concept;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.authentication.domain.policy.PolicyCheckReport;
import life.qbic.authentication.domain.policy.PolicyStatus;
import life.qbic.authentication.domain.user.policy.EmailFormatPolicy;

/**
 * <b>EmailAddress</b>
 * <p>
 * Represents a valid mail value, specified by RFC5322.
 * <p>
 * For mor details please check {@link EmailFormatPolicy}.
 *
 * @since 1.0.0
 */
public class EmailAddress implements Serializable {

  @Serial
  private static final long serialVersionUID = -2988567868530531076L;

  private final String value;

  /**
   * Creates an mail object instance from a String representation of an mail value.
   * <p>
   * This method performs a format validation.
   *
   * @param s the mail value String
   * @return an mail
   * @throws EmailValidationException if the mail format is not a valid mail format specified by
   *                                  RFC5322
   * @since 1.0.0
   */
  public static EmailAddress from(String s) throws EmailValidationException {
    PolicyCheckReport policyCheckReport = EmailFormatPolicy.instance().validate(s);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new EmailValidationException(policyCheckReport, s, "Invalid mail address format");
    }
    return new EmailAddress(s);
  }

  private EmailAddress(String emailAddress) {
    super();
    this.value = emailAddress;
  }

  /**
   * Queries the mail value as String representation.
   *
   * @return mail value as String
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
    EmailAddress emailAddress = (EmailAddress) o;
    return value.equals(emailAddress.get());
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }


  /**
   * <h1>Exception that indicates violations during the mail value validation process</h1>
   *
   * <p>This exception is supposed to be thrown, if the provided mail value for an user violates
   * the format specified by RFC5322 It's intention is to contain the invalid mail value and the
   * PolicyCheckReport for the violated policy</p>
   *
   * @since 1.0.0
   */
  public static class EmailValidationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -4253849498611530692L;

    private final String invalidEmailAddress;

    private final transient PolicyCheckReport emailPolicyCheckReport;

    EmailValidationException(PolicyCheckReport emailAddressCheckReport,
        String invalidEmailAddress, String message) {
      super(message);
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
