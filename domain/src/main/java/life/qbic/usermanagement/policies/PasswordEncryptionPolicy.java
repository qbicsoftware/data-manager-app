package life.qbic.usermanagement.policies;

public class PasswordEncryptionPolicy {
  private static PasswordEncryptionPolicy INSTANCE;

  public static PasswordEncryptionPolicy create() {
    if (INSTANCE == null) {
      INSTANCE = new PasswordEncryptionPolicy();
    }
    return INSTANCE;
  }

  public String encrypt(String password) {
    return password;
  }

}
