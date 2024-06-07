package life.qbic.identity.domain.event;

import java.io.Serial;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.FullName;
import life.qbic.identity.domain.model.UserId;

/**
 * <b>Password Reset Domain Event</b>
 *
 * <p>Indicates that a user wants to set a new password.</p>
 *
 * @since 1.0.0
 */
public class PasswordResetRequested extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 7086981262356789842L;

  private final UserId userId;

  private final FullName fullName;

  private final EmailAddress emailAddress;

  /**
   * Creates a new {@link PasswordResetRequested} domain event.
   * <p>
   * The instant of the event will be the time-point of the event instance creation.
   *
   * @param userId the id of the user for whom the password needs to be reset
   * @return a new instance of a password reset domain event
   * @since 1.0.0
   */
  public static PasswordResetRequested create(UserId userId, FullName name,
      EmailAddress emailAddress) {
    return new PasswordResetRequested(userId, name, emailAddress);
  }

  private PasswordResetRequested(UserId userId, FullName name, EmailAddress emailAddress) {
    this.userId = userId;
    this.fullName = name;
    this.emailAddress = emailAddress;
  }

  /**
   * Returns the user id of the user for whom the password needs to be reset
   *
   * @return the affected user's id
   * @since 1.0.0
   */
  public UserId userId() {
    return userId;
  }

  /**
   * Returns the user's registered mail address
   *
   * @return the user's mail address
   * @since 1.0.0
   */
  public EmailAddress userEmailAddress() {
    return emailAddress;
  }

  /**
   * Returns the full name of the user for whom the password reset was requested
   *
   * @return the name of the user
   * @since 1.0.0
   */
  public FullName userFullName() {
    return this.fullName;
  }
}
