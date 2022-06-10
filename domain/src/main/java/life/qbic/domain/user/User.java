package life.qbic.domain.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * <b>User class</b>
 *
 * <p>User aggregate in the context of user management.
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

  @Convert(converter = FullNameConverter.class)
  private FullName fullName;

  @Column(name = "email")
  @Convert(converter = EmailConverter.class)
  private EmailAddress emailAddress;

  @Convert(converter = PasswordConverter.class)
  private EncryptedPassword encryptedPassword;

  private boolean active = false;

  protected User() {
  }

  /**
   * Creates a new user account, with a unique identifier to unambiguously match the user within
   * QBiC's organisation.
   *
   * <p>It is the client's responsibility to reset the raw password, after the user has been
   * created.
   *
   * <p>The object instance won't hold a reference to the original password char array, after it
   * has been encrypted.
   *
   * @param fullName          the full name of the user
   * @param emailAddress      the email address value of the user
   * @param encryptedPassword the encrypted password of the new user
   * @return the new user
   * @since 1.0.0
   */
  public static User create(FullName fullName, EmailAddress emailAddress,
      EncryptedPassword encryptedPassword) {
    String uuid = String.valueOf(UUID.randomUUID());
    var user = new User(fullName);
    user.setEmail(emailAddress);
    user.setId(uuid);
    user.setEncryptedPassword(encryptedPassword);
    user.active = false;

    return user;
  }

  /**
   * Recreates an instance of a user object, for example when loading user data from the persistence
   * layer.
   *
   * @param encryptedPassword the encrypted password
   * @param fullName          the full name
   * @param emailAddress      the emailAddress
   * @return an object instance of the user
   * @since 1.0.0
   */
  protected static User of(EncryptedPassword encryptedPassword, FullName fullName,
      EmailAddress emailAddress) {
    var user = new User(fullName);
    user.setEmail(emailAddress);
    user.setEncryptedPassword(encryptedPassword);
    return user;
  }

  private User(FullName fullName) {
    this.fullName = fullName;
  }

  @Override
  public String toString() {
    return "User{" +
        "id='" + id + '\'' +
        ", fullName=" + fullName +
        ", emailAddress=" + emailAddress +
        ", encryptedPassword=" + encryptedPassword +
        ", active=" + active +
        '}';
  }

  private void setEncryptedPassword(EncryptedPassword encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  /**
   * Get access to the encrypted password
   *
   * @return the password
   * @since 1.0.0
   */
  public EncryptedPassword getEncryptedPassword() {
    return this.encryptedPassword;
  }

  /**
   * Sets the emailAddress value for the current user.
   *
   * @param emailAddress the emailAddress value of the user
   * @since 1.0.0
   */
  private void setEmail(EmailAddress emailAddress) {
    this.emailAddress = emailAddress;
  }

  private void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public EmailAddress getEmail() {
    return this.emailAddress;
  }

  public FullName getFullName() {
    return this.fullName;
  }

}
