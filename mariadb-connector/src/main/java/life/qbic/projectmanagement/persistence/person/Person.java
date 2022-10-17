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
@Table(name = "person")
public class Person {

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  private Long id;

  @Column(name = "email")
  private String email;

  @Column(name = "reference_id")
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
}
