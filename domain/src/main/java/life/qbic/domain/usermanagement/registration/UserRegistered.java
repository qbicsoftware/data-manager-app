package life.qbic.domain.usermanagement.registration;

import java.time.Instant;
import life.qbic.domain.events.DomainEvent;

/**
 * <b>A user registered in the user management context.</b>
 *
 * @since 1.0.0
 */
public class UserRegistered implements DomainEvent {

  private final Instant occurredOn;

  private final String fullName;

  private final String email;

  private final String userId;

  public static UserRegistered createEvent(final String userId, final String fullName, final String email) {
    return new UserRegistered(userId, fullName, email);
  }

  /**
   * @param userId the registered user
   * @since 1.0.0
   */
  private UserRegistered(final String userId, final String fullName, final String email) {
    this.userId = userId;
    this.fullName = fullName;
    this.email = email;
    this.occurredOn = Instant.now();
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public String userId() {
    return userId;
  }

  public String userFullName() {
    return fullName;
  }

  public String userEmail() {
    return email;
  }
}
