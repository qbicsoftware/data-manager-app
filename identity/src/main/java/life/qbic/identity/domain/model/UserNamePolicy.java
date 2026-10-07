package life.qbic.identity.domain.model;

import static java.util.Objects.isNull;

import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.identity.api.UserNames;
import life.qbic.identity.domain.model.policy.PolicyCheckReport;
import life.qbic.identity.domain.model.policy.PolicyStatus;

/**
 * <b>User Name Policy</b>
 *
 * <p>Validates a putative username against the platform's username rules.
 *
 * <p>The username is the user-visible handle shown throughout the application (project sharing,
 * group membership, collaborators). To keep that handle usable in compact UI contexts it is
 * bounded to {@value #MAX_LENGTH} characters.
 *
 * <p><b>Why a policy and not a value object?</b> Usernames are already persisted in the database.
 * A strict value object that rejects values on deserialisation would make history unreadable: a
 * single pre-existing over-long username would make every query loading that {@link User} fail,
 * taking down user pickers, project sharing and the project overview for all users. The policy
 * validates only values that <em>enter</em> the system (registration and username change); the
 * aggregate keeps the username as a plain {@link String} and therefore stays able to represent
 * every state the database can hold. This mirrors the established {@link EmailFormatPolicy} and
 * {@link PasswordPolicy} precedent in this bounded context.
 *
 * @since 1.19.0
 */
public class UserNamePolicy {

  /**
   * The maximum allowed number of characters of a username.
   */
  public static final int MAX_LENGTH = UserNames.MAX_LENGTH;

  private static UserNamePolicy policy;

  public static UserNamePolicy instance() {
    if (policy == null) {
      policy = new UserNamePolicy();
    }
    return policy;
  }

  /**
   * Validates a given putative username.
   *
   * @param userName the username to validate
   * @return a check report with the validation information
   * @since 1.19.0
   */
  public PolicyCheckReport validate(String userName) {
    if (isNull(userName) || userName.isBlank()) {
      return new PolicyCheckReport(PolicyStatus.FAILED, "Username must not be empty.");
    }
    if (userName.strip().length() > MAX_LENGTH) {
      return new PolicyCheckReport(PolicyStatus.FAILED,
          "Username must not exceed " + MAX_LENGTH + " characters.");
    }
    return new PolicyCheckReport(PolicyStatus.PASSED, "");
  }

  /**
   * The maximum allowed number of characters of a username.
   *
   * @return the configured maximum username length
   * @since 1.19.0
   */
  public static int maxLength() {
    return MAX_LENGTH;
  }

  /**
   * Indicates whether the given username exceeds the allowed maximum length.
   *
   * <p>This is used to surface grandfathered usernames (created before the limit was introduced)
   * to their owners, without rejecting them on read or on unrelated writes.
   *
   * @param userName the username to inspect
   * @return {@code true} if the value is longer than {@link #MAX_LENGTH}
   * @since 1.19.0
   */
  public static boolean exceedsMaxLength(String userName) {
    return UserNames.exceedsMaxLength(userName);
  }

  /**
   * <h1>Exception that indicates violations during the username validation process</h1>
   *
   * <p>Thrown when a username entering the system is empty or exceeds {@link #MAX_LENGTH}
   * characters. It carries the invalid value for diagnostics.</p>
   *
   * @since 1.19.0
   */
  public static class UserNameValidationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = 2894561239874561230L;

    private final transient String invalidUserName;

    public UserNameValidationException(String message, String invalidUserName) {
      super(message);
      this.invalidUserName = invalidUserName;
    }

    public String getInvalidUserName() {
      return invalidUserName;
    }
  }
}
