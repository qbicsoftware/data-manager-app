package life.qbic.projectmanagement.finances.offer;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Table;
/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Table(name = "person")
public class Customer {

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  public static Customer from(FullName fullName) {
    Objects.requireNonNull(fullName);
    return new Customer(fullName);
  }

  private Customer(FullName fullName){
    this.firstName = fullName.firstName();
    this.lastName = fullName.lastName();
  }

  private Customer(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }

  private void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  private void setLastName(String lastName) {
    this.lastName = lastName;
  }

}
