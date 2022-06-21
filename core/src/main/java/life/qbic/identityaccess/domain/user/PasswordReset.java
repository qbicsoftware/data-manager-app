package life.qbic.identityaccess.domain.user;

import java.io.Serial;
import java.time.Instant;
import life.qbic.shared.domain.events.DomainEvent;

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

  /**
   * Creates a new {@link PasswordReset} domain event.
   * <p>
   * The instant of the event will be the time-point of the event instance creation.
   *
   * @param userId the id of the user for whom the password needs to be reset
   * @return a new instance of a password reset domain event
   * @since 1.0.0
   */
  public static PasswordReset create(UserId userId) {
    return new PasswordReset(userId, Instant.now());
  }

  private PasswordReset(UserId userId, Instant occurredOn) {
    super();
    this.userId = userId;
    this.occurredOn = occurredOn;
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
}
