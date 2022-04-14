package life.qbic.usermanagement;

import java.util.UUID;
import life.qbic.usermanagement.policies.*;

public class User {

  private String id;

  private String fullName;

  private String email;

  private String encryptedPassword;

  /**
   * Creates a new user account, with a unique identifier to unambiguously match the user within
   * QBiC's organisation.
   *
   * @param password the desired password for the user
   * @param fullName the full name of the user
   * @param email    the email address of the user
   * @return the new user
   * @since 1.0.0
   */
  public static User create(String password, String fullName, String email) throws UserException {
    String uuid = String.valueOf(UUID.randomUUID());
    var user = new User(fullName);
    try {
      user.setEmail(email);
      user.setPassword(password);
      user.setId(uuid);
    } finally {
      password = null;
    }
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

  /**
   * Sets a password for the current user.
   * <p>
   * Beware that the password gets validated against the current password policy. If the password
   * violates the policy, an {@link UserException} is thrown.
   * <p>
   * The password is then stored in an encrypted form, controlled by the
   * {@link PasswordEncryptionPolicy}.
   *
   * @param password the new user password
   * @throws UserException if the user password is too weak
   * @since 1.0.0
   */
  public void setPassword(String password) throws UserException {
    try {
      validatePassword(password);
      this.encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password);
    } finally {
      password = null; // override the clear text password before GC sets in
    }
  }

  private void validatePassword(String password) {
    PolicyCheckReport policyCheckReport = PasswordPolicy.create().validate(password);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new UserException(policyCheckReport.reason());
    }
  }

  protected void setEncryptedPassword(String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
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

  private void validateEmail(String email) throws UserException {
    PolicyCheckReport policyCheckReport = EmailFormatPolicy.create().validate(email);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new UserException(policyCheckReport.reason());
    }
  }

  static class UserException extends RuntimeException {

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
