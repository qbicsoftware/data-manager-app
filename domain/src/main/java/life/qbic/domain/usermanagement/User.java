package life.qbic.domain.usermanagement;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import life.qbic.domain.usermanagement.policies.EmailFormatPolicy;
import life.qbic.domain.usermanagement.policies.PasswordEncryptionPolicy;
import life.qbic.domain.usermanagement.policies.PasswordPolicy;
import life.qbic.domain.usermanagement.policies.PolicyCheckReport;
import life.qbic.domain.usermanagement.policies.PolicyStatus;

/**
 * <b>User class</b>
 * <p>
 * User aggregate in the context of user management.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

  @Serial
  private static final long serialVersionUID = -8469632941022622595L;

  @Id
  @Column(name = "id")
  private String id;

  private String fullName;

  private String email;

  private String encryptedPassword;

  private boolean emailConfirmed;

  protected User() {

  }

  /**
   * Creates a new user account, with a unique identifier to unambiguously match the user within
   * QBiC's organisation.
   * <p>
   * It is the client's responsibility to reset the raw password, after the user has been created.
   * <p>
   * The object instance won't hold a reference to the original password char array, after it has
   * been encrypted.
   *
   * @param fullName    the full name of the user
   * @param email       the email address of the user
   * @return the new user
   * @since 1.0.0
   */
  public static User create(String fullName, String email) throws UserException {
    String uuid = String.valueOf(UUID.randomUUID());
    var user = new User(fullName);
    user.setEmail(email);
    user.setId(uuid);

    return user;
  }

  /**
   * Recreates an instance of a user object, for example when loading user data from the persistence
   * layer.
   *
   * @param encryptedPassword the encrypted password
   * @param fullName          the full name
   * @param email             the email
   * @return an object instance of the user
   * @since 1.0.0
   */
  protected static User of(String encryptedPassword, String fullName, String email) {
    var user = new User(fullName);
    user.setEmail(email);
    user.setEncryptedPassword(encryptedPassword);
    return user;
  }

  private User(String fullName) {
    this.fullName = fullName;
  }

  @Override
  public String toString() {
    return "User{" +
        "id='" + id + '\'' +
        ", fullName='" + fullName + '\'' +
        ", email='" + email + '\'' +
        '}';
  }

  /**
   * Sets a password for the current user.
   * <p>
   * Beware that the password gets validated against the current password policy. If the password
   * violates the policy, an {@link UserException} is thrown.
   * <p>
   * The password is then stored in an encrypted form, controlled by the
   * {@link PasswordEncryptionPolicy}.
   * <p>
   * It is the client's responsibility to reset the raw password, after it has been set for the user
   * and encrypted.
   *
   * @param rawPassword the new user password
   * @throws UserException if the user password is too weak
   * @since 1.0.0
   */
  public void setPassword(char[] rawPassword) throws UserException {
    validatePassword(rawPassword);
    this.encryptedPassword = PasswordEncryptionPolicy.create().encrypt(rawPassword);
  }

  private void validatePassword(char[] rawPassword) {
    PolicyCheckReport policyCheckReport = PasswordPolicy.create().validate(rawPassword);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new UserException(policyCheckReport.reason());
    }
  }

  protected void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  /**
   * Get access to the encrypted password
   *
   * @return the password
   * @since 1.0.0
   */
  public String getEncryptedPassword() {
    return this.encryptedPassword;
  }

  /**
   * Sets the email address for the current user.
   * <p>
   * This method will throw an {@link UserException} if the email address format seems not to be a
   * valid email address. The format policy is specified in {@link EmailFormatPolicy}.
   *
   * @param email the email address of the user
   * @throws UserException if the email address violates the policy
   * @since 1.0.0
   */
  public void setEmail(String email) throws UserException {
    validateEmail(email);
    this.email = email;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public String getEmail() {
    return this.email;
  }

  public String getFullName() {
    return this.fullName;
  }

  public boolean isEmailConfirmed() {
    return this.emailConfirmed;
  }

  public void setEmailConfirmed(boolean emailConfirmed) {
    this.emailConfirmed = emailConfirmed;
  }

  /**
   * Checks if a given password is correct for a user
   *
   * @param rawPassword Password that is being validated
   * @return true, if the given password is correct for the user
   */
  public Boolean checkPassword(char[] rawPassword) {
    return Objects.equals(PasswordEncryptionPolicy.create().encrypt(rawPassword), encryptedPassword);
  }

  private void validateEmail(String email) throws UserException {
    PolicyCheckReport policyCheckReport = EmailFormatPolicy.create().validate(email);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new UserException(policyCheckReport.reason());
    }
  }

  public static class UserException extends RuntimeException {

    private final String reason;

    public UserException() {
      super();
      this.reason = "";
    }

    public UserException(String reason) {
      super(reason);
      this.reason = reason;
    }

    public String getReason() {
      return reason;
    }

  }

}
