package life.qbic.domain.usermanagement;

import java.io.Serial;
import java.time.Instant;
import life.qbic.domain.events.DomainEvent;

/**
 * <b>A user was activated.</b>
 */
public class UserActivated extends DomainEvent {

  public enum Reason {
    EMAIL_CONFIRMED
  }

  public static UserActivated create(final String userId, final Reason reason) {
    return new UserActivated(userId, reason);
  }

  private UserActivated(String userId, Reason reason) {
    this.userId = userId;
    this.reason = reason;
    this.occurredOn = Instant.now();
  }

  @Serial
  private static final long serialVersionUID = -8345773958846846639L;

  private final Instant occurredOn;
  private final String userId;
  private final Reason reason;

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public String userId() {
    return userId;
  }

  public Reason reason() {
    return reason;
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
    if (!userId.equals(that.userId)) {
      return false;
    }
    return reason == that.reason;
  }

  @Override
  public int hashCode() {
    int result = occurredOn.hashCode();
    result = 31 * result + userId.hashCode();
    result = 31 * result + reason.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "UserActivated{" +
        "occurredOn=" + occurredOn +
        ", userId='" + userId + '\'' +
        ", reason=" + reason +
        '}';
  }
}
