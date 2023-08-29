package life.qbic.authentication.domain.user.concept;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import life.qbic.authentication.domain.user.event.PasswordResetRequested;
import life.qbic.authentication.domain.user.event.UserActivated;
import life.qbic.authentication.domain.user.event.UserEmailConfirmed;
import life.qbic.authentication.domain.user.repository.jpa.EmailConverter;
import life.qbic.authentication.domain.user.repository.jpa.FullNameConverter;
import life.qbic.authentication.domain.user.repository.jpa.PasswordConverter;
import life.qbic.domain.concepts.DomainEventDispatcher;

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
    UserId id = UserId.create();
    var user = new User(id, fullName, emailAddress, encryptedPassword);
    user.active = false;

    return user;
  }

  private User(UserId id, FullName fullName, EmailAddress emailAddress,
      EncryptedPassword encryptedPassword) {
    this.id = id;
    this.fullName = fullName;
    this.emailAddress = emailAddress;
    this.encryptedPassword = encryptedPassword;
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
   * Sets the mail address for the current user.
   *
   * @param emailAddress the mail address of the user
   * @since 1.0.0
   */
  private void setEmail(EmailAddress emailAddress) {
    this.emailAddress = emailAddress;
  }

  public UserId id() {
    return this.id;
  }

  public EmailAddress emailAddress() {
    return this.emailAddress;
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
    this.active = true;
    UserActivated event = UserActivated.create(id.get());
    DomainEventDispatcher.instance().dispatch(event);
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
