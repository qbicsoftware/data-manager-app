package life.qbic.identity.domain.model;

import life.qbic.identity.api.Passwords;
import life.qbic.identity.domain.model.policy.PolicyCheckReport;
import life.qbic.identity.domain.model.policy.PolicyStatus;

/**
 * <b>Class PasswordPolicy</b>
 *
 * <p>Length and complexity are the two properties that have an effect on password strength. We do
 * not enforce complexity explicitly, although we advice to use password managers that generate
 * strong passwords. Currently we only enforce the password length to have at least
 * {@value Passwords#MIN_LENGTH} characters or more.
 *
 * <p>The number and the rejection message are taken from {@link Passwords} so that the rule the
 * domain enforces and the rule the forms display cannot diverge. They previously did: this class
 * enforced 12 characters while reporting "shorter than 8 characters".
 *
 * @since 1.0.0
 */
class PasswordPolicy {

  private static final int MIN_LENGTH = Passwords.MIN_LENGTH;

  private static PasswordPolicy policy;

  public static PasswordPolicy instance() {
    if (policy == null) {
      policy = new PasswordPolicy();
    }
    return policy;
  }

  /**
   * Validates the raw password against the current policy.
   *
   * @param rawPassword the password to validate
   * @return a policy check report
   * @since 1.0.0
   */
  public PolicyCheckReport validate(char[] rawPassword) {
    if (rawPassword.length < MIN_LENGTH) {
      return new PolicyCheckReport(PolicyStatus.FAILED, Passwords.tooShortMessage());
    }
    return new PolicyCheckReport(PolicyStatus.PASSED, "");
  }
}
