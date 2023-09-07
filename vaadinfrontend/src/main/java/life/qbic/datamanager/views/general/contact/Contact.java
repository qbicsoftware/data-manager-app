package life.qbic.datamanager.views.general.contact;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public final class Contact {

  private String fullName;
  private String email;

  public Contact(String fullName, String email) {
    requireNonNull(fullName, "fullName must not be null");
    requireNonNull(email, "email must not be null");
    this.fullName = fullName;
    this.email = email;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmail() {
    return email;
  }

  public life.qbic.projectmanagement.domain.project.Contact toDomainContact() {
    return new life.qbic.projectmanagement.domain.project.Contact(getFullName(), getEmail());
  }
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (Contact) obj;
    return Objects.equals(this.fullName, that.fullName) &&
        Objects.equals(this.email, that.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullName, email);
  }

  @Override
  public String toString() {
    return "Contact[" +
        "fullName=" + fullName + ", " +
        "email=" + email + ']';
  }


}
