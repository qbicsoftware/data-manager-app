package life.qbic.projectmanagement.persistence.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  public String fullName() {
    return this.firstName + " " + this.lastName;
  }

  public String referenceId() {
    return this.referenceId;
  }

  public String emailAddress() {
    return this.email;
  }
}
