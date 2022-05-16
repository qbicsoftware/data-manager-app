package life.qbic.usermanagement.registration;

import java.time.Instant;
import life.qbic.events.DomainEvent;


public class UserRegistered implements DomainEvent {

  private Instant occurredOn;

  private final String userId;

  /**
   * @param userId the registered user
   * @since 1.0.0
   */
  public UserRegistered(String userId) {
    this.userId = userId;
    this.occurredOn = Instant.now();
  }

  public void setOccurredOn(Instant occurredOn) {
    this.occurredOn = occurredOn;
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
