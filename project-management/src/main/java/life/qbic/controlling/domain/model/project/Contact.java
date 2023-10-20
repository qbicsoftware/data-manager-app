package life.qbic.controlling.domain.model.project;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Record representing a person reference with name and contact email address
 *
 * @since 1.0.0
 */
@Embeddable
public class Contact {

  private String fullName;

  private String emailAddress;

  public Contact(String fullName, String emailAddress) {
    requireNonNull(fullName, "fullName must not be null");
    requireNonNull(emailAddress, "emailAddress must not be null");
    if (fullName.isBlank()) {
      throw new IllegalArgumentException("A contacts name must not be empty");
    }
    if (emailAddress.isBlank()) {
      throw new IllegalArgumentException("A contacts email must not be empty");
    }
    this.fullName = fullName;
    this.emailAddress = emailAddress;
  }

  protected Contact() {
    // needed for JPA
  }

  public String fullName() {
    return fullName;
  }

  public String emailAddress() {
    return emailAddress;
  }


  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    Contact that = (Contact) object;

    if (!Objects.equals(fullName, that.fullName)) {
      return false;
    }
    return Objects.equals(emailAddress, that.emailAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullName, emailAddress);
  }
}
