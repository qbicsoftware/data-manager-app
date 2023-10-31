package life.qbic.datamanager.views.general.contact;

import static java.util.Objects.requireNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 *
 * <b>A contact person</b>
 *
 * <p>Describes a contact person with a name and email.</p>
 *
 * @since 1.0.0
 */
public final class Contact implements Serializable {

  @Serial
  private static final long serialVersionUID = 8233688580418521324L;
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

  public life.qbic.projectmanagement.domain.model.project.Contact toDomainContact() {
    return new life.qbic.projectmanagement.domain.model.project.Contact(getFullName(), getEmail());
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
