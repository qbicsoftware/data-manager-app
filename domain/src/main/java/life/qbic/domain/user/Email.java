package life.qbic.domain.user;

import java.util.Objects;
import life.qbic.domain.usermanagement.policies.EmailFormatPolicy;
import life.qbic.domain.usermanagement.policies.PolicyCheckReport;
import life.qbic.domain.usermanagement.policies.PolicyStatus;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Email {

  private String address;

  public static Email from(String s) throws EmailValidationException {
    PolicyCheckReport policyCheckReport = EmailFormatPolicy.create().validate(s);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new EmailValidationException(policyCheckReport.reason());
    }
    var email = new Email();
    email.set(s);
    return email;
  }

  private Email() {
    super();
  }

  private void set(String s) {
    address = s;
  }

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
    Email email = (Email) o;
    return address.equals(email.address());
  }

  @Override
  public int hashCode() {
    return Objects.hash(address);
  }

  public static class EmailValidationException extends RuntimeException {

    EmailValidationException() {
      super();
    }

    EmailValidationException(String message) {
      super(message);
    }

  }
}
