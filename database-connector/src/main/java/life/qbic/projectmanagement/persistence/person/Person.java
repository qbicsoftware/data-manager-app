package life.qbic.projectmanagement.persistence.person;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * JPA query object for person information.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "person_datamanager")
public class Person {

  @Column(name = "firstName")
  private String firstName;

  @Column(name = "lastName")
  private String lastName;

  @Id
  private Long id;

  @Column(name = "email")
  private String email;

  @Column(name = "referenceId")
  private String referenceId;

  public void setId(Long id) {
    this.id = id;
  }

  @Id
  public Long getId() {
    return id;
  }

  public String fullName() {
    return this.firstName + " " + this.lastName;
  }

  public String referenceId() {
    return this.referenceId;
  }

  public String emailAddress() {
    return this.email;
  }

  private String getFirstName() {
    return firstName;
  }

  private void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  private String getLastName() {
    return lastName;
  }

  private void setLastName(String lastName) {
    this.lastName = lastName;
  }

  private String getEmail() {
    return email;
  }

  private void setEmail(String email) {
    this.email = email;
  }

  private String getReferenceId() {
    return referenceId;
  }

  private void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }
}
