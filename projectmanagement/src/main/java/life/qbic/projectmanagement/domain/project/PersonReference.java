package life.qbic.projectmanagement.domain.project;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Record representing a person reference with name and contact email address
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "project_references")
public class PersonReference {

  @Column(name = "referenceId")
  private String referenceId;

  @Column(name = "fullName")
  private String fullName;

  @Column(name = "email")
  private String emailAddress;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  public PersonReference(String referenceId, String fullName, String emailAddress) {
    this.referenceId = referenceId;
    this.fullName = fullName;
    this.emailAddress = emailAddress;
  }

  protected PersonReference() {

  }

  private String getReferenceId() {
    return referenceId;
  }

  private String getFullName() {
    return fullName;
  }

  public String fullName() {
    return fullName;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
        fullName, that.fullName) && Objects.equals(emailAddress, that.emailAddress)
        && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(referenceId, fullName, emailAddress, id);
  }
}
