package life.qbic.identityaccess.domain.user.policy;

import life.qbic.identityaccess.domain.PolicyCheckReport;
import life.qbic.identityaccess.domain.PolicyStatus;

/**
 * <b>Class PasswordPolicy</b>
 *
 * <p>Length and complexity are the two properties that have an effect on password strength. We do
 * not enforce complexity explicitly, although we advice to use password managers that generate
 * strong passwords. Currently we only enforce the password length to have at least 8 characters or
 * more.
 *
 * @since 1.0.0
 */
public class PasswordPolicy {

  private static final int MIN_LENGTH = 8;

  private static PasswordPolicy INSTANCE;

  public static PasswordPolicy instance() {
    if (INSTANCE == null) {
      INSTANCE = new PasswordPolicy();
    }
    return INSTANCE;
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
      return new PolicyCheckReport(PolicyStatus.FAILED, "Password shorter than 8 characters.");
    }
    return new PolicyCheckReport(PolicyStatus.PASSED, "");
  }
}
