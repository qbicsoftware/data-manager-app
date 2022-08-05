package life.qbic.authentication.domain.user.policy;

import java.util.regex.Pattern;
import life.qbic.authentication.domain.policy.PolicyCheckReport;
import life.qbic.authentication.domain.policy.PolicyStatus;

/**
 * <b>EmailAddress Format Policy</b>
 *
 * <p>Validates a String against the RFC5322 mail format specification (<a
 * href="https://www.rfc-editor.org/rfc/rfc5322">https://www.rfc-editor.org/rfc/rfc5322</a>).
 *
 * @since 1.0.0
 */
public class EmailFormatPolicy {

  /*
  Many thanks to https://www.javatpoint.com/java-email-validation
   */
  private static final String FULL_ADDRESS_SPEC =
      "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*+@(?:[a-zA-Z0-9-]+\\.)+?[a-zA-Z]{2,6}$";

  private static EmailFormatPolicy INSTANCE;

  public static EmailFormatPolicy instance() {
    if (INSTANCE == null) {
      INSTANCE = new EmailFormatPolicy();
    }
    return INSTANCE;
  }

  /**
   * Validates a given putative mail address against the RFC5322 address-spec.
   *
   * @param email the mail to validate
   * @return a check report with the validation information
   * @since 1.0.0
   */
  public PolicyCheckReport validate(String email) {
    if (!honoursRFCSpec(email)) {
      return new PolicyCheckReport(PolicyStatus.FAILED, "Invalid mail address format.");
    }
    return new PolicyCheckReport(PolicyStatus.PASSED, "");
  }

  private static boolean honoursRFCSpec(String email) {
    var pattern = Pattern.compile(FULL_ADDRESS_SPEC);
    var matcher = pattern.matcher(email);
    return matcher.matches();
  }
}
