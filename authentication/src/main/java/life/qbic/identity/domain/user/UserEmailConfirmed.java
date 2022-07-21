package life.qbic.identity.domain.user;

import java.io.Serial;
import java.time.Instant;
import life.qbic.identity.domain.events.DomainEvent;

/**
 * <b>A user email was confirmed</b>
 *
 * @since 1.0.0
 */
public class UserEmailConfirmed extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 3906040782719502015L;

  private final String userId;
  private final String email;
  private final Instant occurredOn;

  public static UserEmailConfirmed create(String userId, String email) {
    return new UserEmailConfirmed(userId, email, Instant.now());
  }

  /**
   * @param userId     the id of the user for which the email was confirmed
   * @param email      the confirmed email address
   * @param occurredOn the timestamp this event happened
   * @since 1.0.0
   */
  private UserEmailConfirmed(final String userId, final String email, Instant occurredOn) {
    this.userId = userId;
    this.email = email;
    this.occurredOn = occurredOn;
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public String userId() {
    return userId;
  }

  public String email() {
    return email;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserEmailConfirmed that)) {
      return false;
    }

    if (!userId.equals(that.userId)) {
      return false;
    }
    if (!email.equals(that.email)) {
      return false;
    }
    return occurredOn.equals(that.occurredOn);
  }

  @Override
  public int hashCode() {
    int result = userId.hashCode();
    result = 31 * result + email.hashCode();
    result = 31 * result + occurredOn.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "UserEmailConfirmed{" +
        "userId='" + userId + '\'' +
        ", email='" + email + '\'' +
        ", occurredOn=" + occurredOn +
        '}';
  }
}
