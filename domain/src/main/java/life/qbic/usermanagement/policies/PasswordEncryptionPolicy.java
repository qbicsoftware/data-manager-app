package life.qbic.usermanagement.policies;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PasswordEncryptionPolicy {
  private static PasswordEncryptionPolicy INSTANCE;

  public static PasswordEncryptionPolicy create() {
    if (INSTANCE == null) {
      INSTANCE = new PasswordEncryptionPolicy();
    }
    return INSTANCE;
  }

  public String encrypt(String password) {
    //Todo implement a sophisticated encryption like AES with a 256-bit key
    return new String(Base64.getEncoder().encode(password.getBytes(StandardCharsets.UTF_8)));
  }

}
