package life.qbic.usermanagement.policies;

public class PasswordPolicy {
  private static PasswordPolicy INSTANCE;

  public static PasswordPolicy create(){
    if (INSTANCE == null) {
      INSTANCE = new PasswordPolicy();
    }
    return INSTANCE;
  }

  public PolicyCheckReport validate(String password) {
    return new PolicyCheckReport(PolicyStatus.FAILED, "Not implemented");
  }

}
