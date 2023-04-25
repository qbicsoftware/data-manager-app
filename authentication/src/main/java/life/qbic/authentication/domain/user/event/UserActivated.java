package life.qbic.authentication.domain.user.event;

import life.qbic.domain.concepts.DomainEvent;

import java.io.Serial;
import java.time.Instant;


/**
 * <b>A user was activated.</b>
 */
public class UserActivated extends DomainEvent {

  public static UserActivated create(final String userId) {
    return new UserActivated(userId);
  }

  private UserActivated(String userId) {
    this.userId = userId;
    this.occurredOn = Instant.now();
  }

  @Serial
  private static final long serialVersionUID = -8345773958846846639L;

  private final Instant occurredOn;
  private final String userId;

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public String userId() {
    return userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserActivated that)) {
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

  @Override
  public String toString() {
    return "UserActivated{" +
        "occurredOn=" + occurredOn +
        ", userId='" + userId + '\'' +
        '}';
  }
}
