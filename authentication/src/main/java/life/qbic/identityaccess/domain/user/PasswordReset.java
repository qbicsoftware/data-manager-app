package life.qbic.identityaccess.domain.user;

import java.io.Serial;
import java.time.Instant;
import life.qbic.identityaccess.domain.events.DomainEvent;

/**
 * <b>Password Reset Domain Event</b>
 *
 * <p>Indicates that a user wants to set a new password.</p>
 *
 * @since 1.0.0
 */
public class PasswordReset extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 7086981262356789842L;

  private final UserId userId;

  private final Instant occurredOn;

  private final FullName fullName;

  private final EmailAddress emailAddress;

  /**
   * Creates a new {@link PasswordReset} domain event.
   * <p>
   * The instant of the event will be the time-point of the event instance creation.
   *
   * @param userId the id of the user for whom the password needs to be reset
   * @return a new instance of a password reset domain event
   * @since 1.0.0
   */
  public static PasswordReset create(UserId userId, FullName name, EmailAddress emailAddress) {
    return new PasswordReset(userId, Instant.now(), name, emailAddress);
  }

  private PasswordReset(UserId userId, Instant occurredOn, FullName name,
      EmailAddress emailAddress) {
    super();
    this.userId = userId;
    this.occurredOn = occurredOn;
    this.fullName = name;
    this.emailAddress = emailAddress;
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
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
   * Returns the user's registered email address
   *
   * @return the user's email address
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
