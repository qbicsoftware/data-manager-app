package life.qbic.authentication.domain.user.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import life.qbic.domain.concepts.DomainEvent;

import java.io.Serial;
import java.time.Instant;

/**
 * <b>A user registered in the user management context.</b>
 *
 * @since 1.0.0
 */
public class UserRegistered extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 2581827831168895067L;

    private Instant occurredOn;

    private String fullName;

    private String email;

    private String userId;

    private UserRegistered() {

    }

    public static UserRegistered create(final String userId, final String fullName,
                                        final String email) {
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

    @JsonGetter("occurredOn")
    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    @JsonGetter("userId")
    public String userId() {
        return userId;
    }

    @JsonGetter("fullName")
    public String userFullName() {
        return fullName;
    }

    @JsonGetter("email")
    public String userEmail() {
        return email;
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
        if (!fullName.equals(that.fullName)) {
            return false;
        }
        if (!email.equals(that.email)) {
            return false;
        }
        return userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        int result = occurredOn.hashCode();
        result = 31 * result + fullName.hashCode();
        result = 31 * result + email.hashCode();
        result = 31 * result + userId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "UserRegistered{" +
            "occurredOn=" + occurredOn +
            ", fullName='" + fullName + '\'' +
            ", email='" + email + '\'' +
            ", userId='" + userId + '\'' +
            '}';
    }
}
