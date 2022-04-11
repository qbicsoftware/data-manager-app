package life.qbic.usermanagement;

import java.util.UUID;
import life.qbic.usermanagement.policies.*;

public class User {

  private String id;

  private String fullName;

  private String email;

  private String encryptedPassword;

  public static User create(String password, String fullName, String email) {
    String uuid = String.valueOf(UUID.randomUUID());
    var user = new User(fullName);
    user.setEmail(email);
    user.setPassword(password);
    user.setId(uuid);
    return user;
  }

  protected static User of(String encryptedPassword, String fullName, String email) {
    var user = new User(fullName);
    user.setEmail(email);
    user.setEncryptedPassword(encryptedPassword);
    return user;
  }

  private User(String fullName) {
    this.fullName = fullName;
  }

  public void setPassword(String password) {
    validatePassword(password);
    this.encryptedPassword = PasswordEncryptionPolicy.create().encrypt(password);
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

  public void setEmail(String email) {
    validateEmail(email);
    this.email = email;
  }

  public void setId(String id) {
    this.id = id;
  }

  private void validateEmail(String email) throws UserException {
    PolicyCheckReport policyCheckReport = EmailFormatPolicy.create().validate(email);
    if (policyCheckReport.status() == PolicyStatus.FAILED) {
      throw new UserException(policyCheckReport.reason());
    }
  }

  private String encryptPassword() {
    return "";
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
