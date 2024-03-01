package life.qbic.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.identity.domain.event.PasswordResetRequested;
import life.qbic.identity.domain.event.UserActivated;
import life.qbic.identity.domain.event.UserEmailConfirmed;
import life.qbic.identity.domain.model.translation.EmailConverter;
import life.qbic.identity.domain.model.translation.FullNameConverter;
import life.qbic.identity.domain.model.translation.PasswordConverter;

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

  @EmbeddedId
  private UserId id;

  @Convert(converter = FullNameConverter.class)
  private FullName fullName;

  @Column(name = "userName")
  private String userName;

  @Column(name = "email")
  @Convert(converter = EmailConverter.class)
  private EmailAddress emailAddress;

  @Convert(converter = PasswordConverter.class)
  private EncryptedPassword encryptedPassword;

  private boolean active = false;

  protected User() {
  }

  private User(UserId id, FullName fullName, EmailAddress emailAddress,
      String userName, EncryptedPassword encryptedPassword) {
    this.id = id;
    this.fullName = fullName;
    this.emailAddress = emailAddress;
    this.encryptedPassword = encryptedPassword;
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
   * @param userName          the desired username
   * @param encryptedPassword the encrypted password of the new user
   * @return the new user
   * @since 1.0.0
   */
  public static User create(FullName fullName, EmailAddress emailAddress,
      String userName, EncryptedPassword encryptedPassword) {
    UserId id = UserId.create();
    var user = new User(id, fullName, emailAddress, userName, encryptedPassword);
    user.active = false;

    return user;
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

  /**
   * Get access to the encrypted password
   *
   * @return the password
   * @since 1.0.0
   */
  public EncryptedPassword getEncryptedPassword() {
    return this.encryptedPassword;
  }

  private void setEncryptedPassword(EncryptedPassword encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
  }

  public UserId id() {
    return this.id;
  }

  public EmailAddress emailAddress() {
    return this.emailAddress;
  }

  public String userName() {
    return userName;
  }

  public FullName fullName() {
    return this.fullName;
  }

  public boolean isActive() {
    return this.active;
  }

  /**
   * Confirms the email address.
   */
  public void confirmEmail() {
    UserEmailConfirmed event = UserEmailConfirmed.create(id.get(), emailAddress.get());
    DomainEventDispatcher.instance().dispatch(event);
    activate();
  }

  /**
   * Requests a password reset.
   * <p>
   * Calling this method will publish a {@link PasswordResetRequested} domain event.
   *
   * @since 1.0.0
   */
  public void resetPassword() {
    PasswordResetRequested event = PasswordResetRequested.create(id, fullName, emailAddress);
    DomainEventDispatcher.instance().dispatch(event);
  }

  /**
   * Overrides the previous password and sets a new one.
   *
   * @param newPassword the new user password
   * @since 1.0.0
   */
  public void setNewPassword(EncryptedPassword newPassword) {
    this.setEncryptedPassword(newPassword);
  }

  private void activate() {
    if (this.active) {
      return;
    }
    this.active = true;
    DomainEventDispatcher.instance().dispatch(UserActivated.create(id.get()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    User user = (User) o;

    return id.equals(user.id());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

}
