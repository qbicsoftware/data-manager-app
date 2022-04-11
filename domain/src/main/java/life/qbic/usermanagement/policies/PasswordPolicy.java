package life.qbic.usermanagement.policies;

/**
 * <b>Class PasswordPolicy</b>
 * <p>Length and complexity are the two properties that have an effect on password strength.
 * We do not enforce complexity explicitly, although we advice to use password managers that generate
 * strong passwords.
 * </p>
 * Currently we only enforce the password length to have at least 8 characters or more.
 * @since 1.0.0
 */
public class PasswordPolicy {

  private static int MIN_LENGTH = 8;

  private static PasswordPolicy INSTANCE;

  public static PasswordPolicy create(){
    if (INSTANCE == null) {
      INSTANCE = new PasswordPolicy();
    }
    return INSTANCE;
  }

  public PolicyCheckReport validate(String password) {
    if (password.trim().length() < MIN_LENGTH) {
      return new PolicyCheckReport(PolicyStatus.FAILED, "Password shorter than 8 characters.");
    }
    return new PolicyCheckReport(PolicyStatus.PASSED, "");
  }

}
