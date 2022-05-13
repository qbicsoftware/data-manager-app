package life.qbic.usermanagement.registration;

import java.time.Instant;
import life.qbic.events.DomainEvent;

/**
 * <b>A user registered int the system.</b>
 *
 * @since 1.0.0
 */
public class UserRegistered implements DomainEvent {

  private final Instant occurredOn;

  private final String userId;

  /**
   * @param userId the registered user
   * @since 1.0.0
   */
  public UserRegistered(String userId) {
    this.userId = userId;
    this.occurredOn = Instant.now();
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public String getUserId() {
    return userId;
  }
}
