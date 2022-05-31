package life.qbic.domain.usermanagement.policies;

/**
 * <b>Full Name Policy</b>
 *
 * <p>Validates if a String is concurrent with the full name requirements
 *
 * @since 1.0.0
 */
public class FullNamePolicy {

  private static final int MIN_LENGTH = 1;

  private static FullNamePolicy INSTANCE;

  public static FullNamePolicy create() {
    if (INSTANCE == null) {
      INSTANCE = new FullNamePolicy();
    }
    return INSTANCE;
  }

  /**
   * Validates the full name against the current policy.
   *
   * @param fullName the full name to validate
   * @return a policy check report
   * @since 1.0.0
   */
  public PolicyCheckReport validate(String fullName) {
    if (fullName.length() < MIN_LENGTH) {
      return new PolicyCheckReport(PolicyStatus.FAILED, "Full Name shorter than 1 character.");
    }
    return new PolicyCheckReport(PolicyStatus.PASSED, "");
  }
}
