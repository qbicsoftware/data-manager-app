package life.qbic.identity.domain.model;

import jakarta.persistence.Column;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * <b>User Id</b>
 * <p>
 * A unique identifier of a user in the user management context.
 *
 * @since 1.0.0
 */
public class UserId implements Serializable {

  @Serial
  private static final long serialVersionUID = 7424966007190805342L;

  @Column(name = "id")
  private String value;

  protected UserId() {
    super();
  }

  /**
   * Creates a new random user id
   *
   * @return a user id
   * @since 1.0.0
   */
  public static UserId create() {
    return new UserId(UUID.randomUUID());
  }

  /**
   * Creates a user id from a String based representation.
   * <p>
   * Throws an {@link IllegalArgumentException} if the input is not a valid user id.
   *
   * @param s a user id String
   * @return an instance of a new user id object
   * @since 1.0.0
   */
  public static UserId from(String s) throws IllegalArgumentException {
    try {
      return new UserId(UUID.fromString(s));
    } catch (IllegalArgumentException ignored) {
      throw new IllegalArgumentException(s + " has unknown user id format.");
    }
  }

  private UserId(UUID id) {
    super();
    this.value = id.toString();
  }

  /**
   * Queries the String value of the user id.
   *
   * @return the user id value
   * @since 1.0.0
   */
  public String get() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserId userId = (UserId) o;
    return Objects.equals(value, userId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "UserId{" +
        "value=" + value +
        '}';
  }
}
