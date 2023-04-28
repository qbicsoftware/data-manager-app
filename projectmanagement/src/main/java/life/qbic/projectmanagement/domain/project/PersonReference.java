package life.qbic.projectmanagement.domain.project;

import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Record representing a person reference with name and contact email address
 *
 * @since 1.0.0
 */
@Embeddable
public class PersonReference {

  private String referenceId;

  private String fullName;

  private String emailAddress;

  public PersonReference(String referenceId, String fullName, String emailAddress) {
    this.referenceId = referenceId;
    this.fullName = fullName;
    this.emailAddress = emailAddress;
  }

  protected PersonReference() {
    // needed for JPA
  }

  public String referenceId() {
    return referenceId;
  }

  public String fullName() {
    return fullName;
  }

  public String emailAddress() {
    return emailAddress;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PersonReference that = (PersonReference) o;
    return Objects.equals(referenceId, that.referenceId) && Objects.equals(
        fullName, that.fullName) && Objects.equals(emailAddress, that.emailAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(referenceId, fullName, emailAddress);
  }
}
