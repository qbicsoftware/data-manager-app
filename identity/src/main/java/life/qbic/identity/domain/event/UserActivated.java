package life.qbic.identity.domain.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import life.qbic.domain.concepts.DomainEvent;


/**
 * <b>A user was activated.</b>
 */
public class UserActivated extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 3828138830336304668L;


  public static UserActivated create(final String userId) {
    return new UserActivated(userId);
  }

  private UserActivated() {
  }

  private UserActivated(String userId) {
    this.userId = userId;
    this.occurredOn = Instant.now();
  }

  private Instant occurredOn;
  private String userId;

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  @JsonGetter("userId")
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
