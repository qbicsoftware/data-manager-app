package life.qbic.usermanagement.registration;

import java.io.Serializable;
import java.time.Instant;
import life.qbic.events.DomainEvent;


public class UserRegistered implements DomainEvent, Serializable {

  private final Instant occurredOn;

  private final String userId;

  /**
   * @param userId the registered user
   * @param occurredOn the timestamp of event occurrence
   * @since 1.0.0
   */
  private UserRegistered(String userId, Instant occurredOn) {
    this.userId = userId;
    this.occurredOn = occurredOn;
  }

  public static UserRegistered create(String userId) {
    return new UserRegistered(userId, Instant.now());
  }
  public static UserRegistered create(String userId, Instant occurredOn) {
    return new UserRegistered(userId, occurredOn);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public String getUserId() {
    return userId;
  }

  @Override
  public String toString() {
    return "UserRegistered{" +
        "occurredOn=" + occurredOn +
        ", userId='" + userId + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserRegistered that)) {
      return false;
    }

    if (!occurredOn.equals(that.occurredOn)) {
      return false;
    }
    return userId.equals(that.userId);
  }

  @Override
  public int hashCode() {
    int result = occurredOn.hashCode();
    result = 31 * result + userId.hashCode();
    return result;
  }
}
